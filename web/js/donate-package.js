(function(global){
    'use strict';
    var data=null,draft=null,active=false,busy=false,saving=false,dirty=false,sequence=0;
    function el(id){return document.getElementById(id);}
    function esc(v){return String(v==null?'':v).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}
    function fold(v){return String(v).toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d');}
    function toast(message,kind){global.Panel.toast(message,kind||'success');}
    function item(id){return data.catalog.items.find(function(i){return i.id===id;})||{id:id,name:'Không tìm thấy #'+id,icon:-1};}
    function icon(id){var i=item(id);return '<button type="button" class="lq-icon" data-dp-icon="'+id+'" title="Xem icon '+esc(i.name)+'" aria-label="Xem icon '+esc(i.name)+'">'+(i.icon>=0?'<img src="/game-data/icon/x1/'+i.icon+'.png" alt="" loading="lazy">':'?')+'</button>';}
    function summary(options){return options.map(function(o){var t=data.catalog.options.find(function(v){return v.id===o.id;});return (t?t.name:'Option #'+o.id+' = #').replace(/#/g,String(o.param));}).join(', ');}
    function lock(value){busy=value;el('dpReload').disabled=value;el('dpCreate').disabled=value||!data;el('dpRows').querySelectorAll('button').forEach(function(b){var p=data&&data.packages.find(function(p){return p.id===Number(b.dataset.dpDelete);});b.disabled=value||!!(p&&p.sold>0);});el('dpForm').querySelectorAll('input,select,textarea,button').forEach(function(b){b.disabled=value;});el('dpClose').disabled=value;}
    async function load(){
        if(busy||saving)return;var request=++sequence;lock(true);el('dpStatus').textContent='Đang tải gói donate…';
        try{var result=await global.PanelAPI.donatePackage.info();if(!active||request!==sequence)return;data=result;el('dpNotes').textContent=result.notes;render();}
        catch(e){if(active&&request===sequence){el('dpStatus').textContent=e.message;toast(e.message,'error');}}
        finally{if(request===sequence)lock(false);}
    }
    function render(){
        if(!data)return;var q=fold(el('dpFilter').value.trim()),state=el('dpActiveFilter').value;
        var rows=data.packages.filter(function(p){return (!q||String(p.id).includes(q)||fold(p.name).includes(q))&&(state==='all'||p.active===(state==='on'));});
        el('dpStatus').textContent=data.packages.length+' gói · '+data.packages.filter(function(p){return p.active;}).length+' đang bán';
        el('dpRows').innerHTML=rows.map(function(p){return '<tr><td>#'+p.id+'<small class="dp-muted">Thứ tự '+p.sortOrder+'</small></td><td><b>'+esc(p.name)+'</b><small class="dp-muted">'+esc(p.detail)+'</small>'+(p.warning?'<small class="dp-warning">'+esc(p.warning)+'</small>':'')+'</td><td>'+Number(p.costVnd).toLocaleString('vi-VN')+'</td><td><div class="dp-thumbs">'+p.items.slice(0,5).map(function(r){return icon(r.itemId)+'<small>×'+Number(r.quantity).toLocaleString('vi-VN')+'</small>';}).join('')+(p.items.length>5?'<small>+'+(p.items.length-5)+' món</small>':'')+'</div></td><td>'+p.sold+'</td><td>'+(p.active?'Đang bán':'Đã tắt')+'</td><td><div class="dp-actions"><button class="btn-mini" type="button" data-dp-edit="'+p.id+'">Sửa</button><button class="btn-mini" type="button" data-dp-toggle="'+p.id+'">'+(p.active?'Tắt':'Bật')+'</button><button class="btn-mini btn-mini--danger" type="button" data-dp-delete="'+p.id+'"'+(p.sold>0?' disabled title="Đã có người mua; tắt gói để giữ lịch sử"':'')+'>Xóa</button></div></td></tr>';}).join('');
        el('dpEmpty').hidden=rows.length>0;el('dpEmpty').textContent=data.packages.length?'Không có gói khớp bộ lọc.':'Chưa có gói donate. Bấm “Thêm gói donate” để tạo.';
    }
    function changed(){dirty=true;el('dpEditStatus').textContent='Có thay đổi chưa lưu.';}
    function open(pack){
        if(busy||!data)return;
        draft=pack?JSON.parse(JSON.stringify(pack)):{name:'',detail:'',costVnd:10000,sortOrder:0,active:true,items:[]};dirty=false;
        el('dpTitle').textContent=pack?'Sửa gói donate #'+pack.id:'Thêm gói donate';
        el('dpName').value=draft.name;el('dpDetail').value=draft.detail;el('dpCost').value=draft.costVnd;el('dpSort').value=draft.sortOrder;el('dpActive').checked=draft.active;
        el('dpWarning').hidden=!draft.warning;el('dpWarningText').textContent=(draft.warning||'')+(draft.warning?' Dữ liệu cũ được giữ nguyên cho đến khi lưu. Kiểm tra và thêm lại các món quà cần sửa.':'');el('dpRaw').textContent=draft.rawItems||'';
        el('dpItemSearch').value='';el('dpEditStatus').textContent='';rewards();search();el('dpEditor').showModal();el('dpName').focus();
    }
    function close(){if(busy||saving)return;if(dirty&&!global.confirm('Bỏ thay đổi chưa lưu của gói donate?'))return;draft=null;dirty=false;el('dpEditor').close();}
    function rewards(){
        el('dpRewards').innerHTML=draft.items.map(function(r,index){return '<div class="dp-reward" data-dp-reward="'+index+'"><div class="lq-reward">'+icon(r.itemId)+'<span class="lq-reward-name">'+esc(item(r.itemId).name)+' <small>#'+r.itemId+'</small><small>'+esc(summary(r.options)||'Không có option bổ sung')+'</small></span><label>Số lượng<input type="number" min="1" max="2147483647" step="1" required data-dp-quantity="'+index+'" value="'+r.quantity+'"></label><button class="btn-mini btn-mini--danger" type="button" data-dp-remove="'+index+'">Xóa món</button></div><details><summary>Chỉ số options ('+r.options.length+')</summary><div class="dp-options">'+r.options.map(function(o,n){return optionRow(o,index,n);}).join('')+'</div><button class="btn-mini" type="button" data-dp-add-option="'+index+'">+ Thêm option</button></details></div>';}).join('')||'<p class="field__hint">Chưa có phần thưởng. Tìm vật phẩm bên dưới để thêm.</p>';
    }
    function optionRow(o,index,n){
        var exists=data.catalog.options.some(function(t){return t.id===o.id;});
        return '<div class="lq-option-row"><select aria-label="Option" data-dp-option="'+index+'" data-index="'+n+'">'+(!exists?'<option value="'+o.id+'">Không tìm thấy #'+o.id+'</option>':'')+data.catalog.options.filter(function(t){return t.id<=255;}).map(function(t){return '<option value="'+t.id+'"'+(t.id===o.id?' selected':'')+'>'+t.id+' · '+esc(t.name)+'</option>';}).join('')+'</select><input type="number" min="0" max="65535" step="1" required aria-label="Giá trị option" data-dp-param="'+index+'" data-index="'+n+'" value="'+o.param+'"><button class="btn-mini" type="button" data-dp-remove-option="'+index+'" data-index="'+n+'" aria-label="Xóa option">×</button></div>';
    }
    function search(){
        if(!data||!draft)return;var q=fold(el('dpItemSearch').value.trim());
        var found=q?data.catalog.items.filter(function(i){return i.id>=0&&i.id<=32767&&(String(i.id).includes(q)||fold(i.name).includes(q));}).sort(function(a,b){return (String(b.id)===q?1:0)-(String(a.id)===q?1:0);}).slice(0,20):[];
        el('dpItemResults').innerHTML=found.map(function(i){return '<div class="lq-search-row">'+icon(i.id)+'<button type="button" class="lq-pick" data-dp-pick="'+i.id+'">'+esc(i.name)+' <small>#'+i.id+'</small><br><small>+ Thêm vào gói</small></button></div>';}).join('')||(q?'<p>Không tìm thấy vật phẩm.</p>':'');
    }
    function preview(id){
        var selected=item(id),frame=el('dpIconFrame'),status=el('dpIconStatus');el('dpIconTitle').textContent=selected.name+' · #'+id;frame.replaceChildren();status.textContent='Đang tải icon…';el('dpIconDialog').showModal();if(selected.icon<0){status.textContent='Vật phẩm chưa có icon.';return;}
        var img=new Image(),scale=4;img.alt=selected.name;img.hidden=true;img.onload=function(){if(frame.firstElementChild===img){img.hidden=false;status.textContent='';}};img.onerror=function(){if(frame.firstElementChild!==img)return;if(--scale>0)img.src='/game-data/icon/x'+scale+'/'+selected.icon+'.png';else status.textContent='Không tải được icon.';};frame.appendChild(img);img.src='/game-data/icon/x4/'+selected.icon+'.png';
    }
    async function mutate(action,body){
        if(busy||saving)return;var request=sequence;saving=true;lock(true);
        try{var result=await global.PanelAPI.donatePackage.run(action,body);toast(result.message);if(action==='create'||action==='update'){draft=null;dirty=false;el('dpEditor').close();}if(active&&request===sequence){busy=false;saving=false;await load();}}
        catch(e){toast(e.message,'error');if(draft)el('dpEditStatus').textContent=e.message;else el('dpStatus').textContent=e.message;}
        finally{saving=false;if(active){lock(false);if(request!==sequence)load();}else busy=false;}
    }
    function onClick(e){
        var b=e.target.closest('button');if(!b||busy||saving||!data)return;
        if(b.dataset.dpIcon!==undefined){preview(Number(b.dataset.dpIcon));return;}
        if(b.dataset.dpPick!==undefined){if(draft.items.length>=100){toast('Mỗi gói tối đa 100 vật phẩm.','error');return;}draft.items.push({itemId:Number(b.dataset.dpPick),quantity:1,options:[]});rewards();changed();return;}
        if(b.dataset.dpRemove!==undefined){draft.items.splice(Number(b.dataset.dpRemove),1);rewards();changed();return;}
        if(b.dataset.dpAddOption!==undefined){var r=draft.items[Number(b.dataset.dpAddOption)],next=data.catalog.options.find(function(t){return t.id<=255&&!r.options.some(function(o){return o.id===t.id;});});if(!next){toast('Không còn option để thêm.','error');return;}r.options.push({id:next.id,param:0});rewards();el('dpRewards').children[Number(b.dataset.dpAddOption)].querySelector('details').open=true;changed();return;}
        if(b.dataset.dpRemoveOption!==undefined){var index=Number(b.dataset.dpRemoveOption);draft.items[index].options.splice(Number(b.dataset.index),1);rewards();el('dpRewards').children[index].querySelector('details').open=true;changed();return;}
        var id=Number(b.dataset.dpEdit||b.dataset.dpToggle||b.dataset.dpDelete),p=data.packages.find(function(v){return v.id===id;});if(!p)return;
        if(b.dataset.dpEdit)open(p);
        else if(b.dataset.dpToggle)mutate('toggle',{id:p.id,revision:p.revision,active:!p.active});
        else if(b.dataset.dpDelete&&p.sold===0&&global.confirm('Xóa gói “'+p.name+'”?'))mutate('delete',{id:p.id,revision:p.revision});
    }
    el('view-donate-package').onclick=onClick;el('dpEditor').addEventListener('click',onClick);
    el('dpRewards').addEventListener('input',function(e){
        if(!draft||busy)return;var t=e.target;
        if(t.dataset.dpQuantity!==undefined)draft.items[Number(t.dataset.dpQuantity)].quantity=Number(t.value);
        else if(t.dataset.dpOption!==undefined)draft.items[Number(t.dataset.dpOption)].options[Number(t.dataset.index)].id=Number(t.value);
        else if(t.dataset.dpParam!==undefined)draft.items[Number(t.dataset.dpParam)].options[Number(t.dataset.index)].param=Number(t.value);
        else return;changed();
    });
    el('dpForm').addEventListener('input',function(e){if(e.target.id!=='dpItemSearch')changed();});
    el('dpForm').onsubmit=function(e){
        e.preventDefault();if(!draft||busy)return;var body={name:el('dpName').value.trim(),detail:el('dpDetail').value,costVnd:Number(el('dpCost').value),sortOrder:Number(el('dpSort').value),active:el('dpActive').checked,items:draft.items};
        if(!body.name||!body.items.length){toast('Nhập tên gói và thêm ít nhất một vật phẩm.','error');return;}
        for(var r of body.items){var seen=new Set();for(var o of r.options){if(seen.has(o.id)){toast('Một món có option trùng ID.','error');return;}seen.add(o.id);}}
        if(draft.id){body.id=draft.id;body.revision=draft.revision;}mutate(draft.id?'update':'create',body);
    };
    el('dpCreate').onclick=function(){open(null);};el('dpReload').onclick=load;el('dpFilter').oninput=render;el('dpActiveFilter').onchange=render;el('dpItemSearch').oninput=search;
    el('dpClose').onclick=close;el('dpCancel').onclick=close;el('dpEditor').addEventListener('cancel',function(e){e.preventDefault();close();});el('dpIconClose').onclick=function(){el('dpIconDialog').close();};
    [el('view-donate-package'),el('dpEditor')].forEach(function(node){node.addEventListener('error',function(e){if(e.target.tagName==='IMG')e.target.hidden=true;},true);});
    global.DonatePackagePanel={start:function(){active=true;if(saving)return;if(draft&&data){el('dpEditor').showModal();lock(false);}else load();},stop:function(){active=false;sequence++;if(!saving)busy=false;el('dpEditor').close();el('dpIconDialog').close();}};
})(window);
