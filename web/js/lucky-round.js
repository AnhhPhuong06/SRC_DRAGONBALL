(function(global){
    'use strict';
    var data=null,draft=null,active=false,busy=false,saving=false,dirty=false,loadError=false,sequence=0,editing=null,pickerTarget=null;
    function el(id){return document.getElementById(id);}
    function esc(v){return String(v==null?'':v).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}
    function clone(v){return JSON.parse(JSON.stringify(v));}
    function toast(message,kind){global.Panel.toast(message,kind||'success');}
    function item(id){return data&&data.catalog.items.find(function(i){return i.id===id;})||{id:id,name:'Không tìm thấy #'+id,icon:-1};}
    function name(id){return esc(item(id).name)+' <small>#'+id+'</small>';}
    function icon(id){var i=item(id);return '<button type="button" class="lq-icon" data-lq-icon="'+id+'" title="Xem icon '+esc(i.name)+'" aria-label="Xem icon '+esc(i.name)+'">'+(i.icon>=0?'<img src="/game-data/icon/x1/'+i.icon+'.png" alt="" loading="lazy">':'?')+'</button>';}
    function lock(value){
        busy=value;el('lqReload').disabled=value;
        el('lqSave').disabled=value||!data||!dirty||loadError;
        el('lqEditor').querySelectorAll('input,select,button').forEach(function(input){input.disabled=value||loadError;});
    }
    function changed(){dirty=true;el('lqStatus').textContent='Có thay đổi chưa lưu.';lock(false);rates();price();}
    async function load(confirmDiscard){
        if(busy||saving)return;
        if(confirmDiscard&&dirty&&!global.confirm('Bỏ các thay đổi chưa lưu và tải lại cấu hình?'))return;
        var request=++sequence;lock(true);el('lqStatus').textContent='Đang tải cấu hình…';
        try {
            var result=await global.PanelAPI.luckyRound.info();if(!active||request!==sequence)return;
            data=result;draft=clone(result.state);dirty=false;loadError=false;
            render();el('lqStatus').textContent='Cấu hình đang áp dụng trong game.';
        }catch(e){if(active&&request===sequence){loadError=true;el('lqStatus').textContent=e.message;toast(e.message,'error');}}
        finally{if(request===sequence)lock(false);}
    }
    function render(){
        el('lqEditor').hidden=false;el('lqNotes').textContent=data.notes;
        var warnings=data.warnings||[];el('lqWarnings').hidden=!warnings.length;el('lqWarnings').innerHTML=warnings.map(function(w){return '<p>'+esc(w)+'</p>';}).join('')+(warnings.length?'<p>Bạn vẫn có thể lưu các mục khác nếu giữ nguyên dòng quà cũ. Để sửa dòng này, thay bằng ID hợp lệ hoặc xóa rồi thêm vật phẩm từ ô tìm kiếm.</p>':'');
        el('lqFields').innerHTML=data.fields.map(function(f){return '<div class="field"><label for="lq_'+f.key+'">'+esc(f.label)+'</label><input id="lq_'+f.key+'" data-lq-value="'+f.key+'" type="number" min="'+f.min+'" max="'+f.max+'" step="1" value="'+draft.values[f.key]+'" required></div>';}).join('');
        el('lqTarget').innerHTML='<option value="price">Vật phẩm dùng để quay</option>'+data.groupFields.map(function(g){return '<option value="group:'+g.key+'">'+esc(g.label)+'</option>';}).join('')+draft.milestones.map(function(m){return '<option value="milestone:'+m.id+'">Quà mốc '+(m.id+1)+'</option>';}).join('');
        groups();milestones();price();rates();search();
    }
    function price(){if(!draft)return;var id=draft.values.idVatPhamQuay;el('lqPrice').innerHTML=icon(id)+'<span>Mỗi lượt dùng <b>'+draft.values.soLuongVatPhamQuay+'</b> '+name(id)+'</span>';}
    function rates(){
        if(!draft)return;var v=draft.values;
        function line(after){var rare=draft.groups[after?'idNhomSieuHiemSauMoc':'idNhomSieuHiem'].length*v.tiLeMoiMonSieuHiem,remaining=(100-rare)/100;
            function pct(n){return Number.isFinite(n)?Number(n.toFixed(4)).toLocaleString('vi-VN')+'%':'—';}
            return '<p><b>'+(after?'Sau':'Trước')+' mốc '+v.mocMoQuaHiem+' lượt:</b> siêu hiếm '+pct(rare)+' · hiếm '+pct(remaining*v.tiLeNhom5)+' · đặc biệt '+pct(remaining*v.tiLeNhom15)+' · thường '+pct(remaining*(100-v.tiLeNhom5-v.tiLeNhom15))+'.</p>';
        }
        el('lqRates').innerHTML='<b>Tỷ lệ thực tế của từng nhóm</b>'+line(false)+line(true)+'<p class="field__hint">Các món trong cùng nhóm được chọn đều nhau. Nhóm thường còn gồm bảng trang bị/nguyên liệu cố định của server.</p>';
    }
    function groups(){el('lqGroups').innerHTML=data.groupFields.map(function(g){return '<div class="lq-group"><div class="lq-group-head"><h4>'+esc(g.label)+'</h4><button class="btn-mini" type="button" data-lq-add="group:'+g.key+'">+ Thêm vật phẩm</button></div>'+(draft.groups[g.key].map(function(id){return '<span class="lq-chip">'+icon(id)+'<span>'+name(id)+'</span><button class="btn-mini" type="button" data-lq-remove-group="'+g.key+'" data-id="'+id+'" aria-label="Xóa '+esc(item(id).name)+'">×</button></span>';}).join('')||'<p class="field__hint">Chưa có vật phẩm. Bấm “Thêm vật phẩm” để thêm vào nhóm này.</p>')+'</div>';}).join('');}
    function optionSummary(options){return options.map(function(o){var template=data.catalog.options.find(function(t){return t.id===o.id;});return (template?template.name:'#'+o.id).replace(/#/g,String(o.param));}).join(', ');}
    function milestones(){el('lqMilestones').innerHTML=draft.milestones.map(function(m){return '<div class="lq-milestone"><div class="lq-milestone-head"><label>Mốc '+(m.id+1)+' — số lượt <input type="number" min="1" max="2147483647" data-lq-spin="'+m.id+'" value="'+m.spins+'" aria-label="Số lượt mốc '+(m.id+1)+'"></label><button class="btn-mini" type="button" data-lq-add="milestone:'+m.id+'">+ Thêm vật phẩm</button></div>'+m.rewards.map(function(r,index){return '<div class="lq-reward">'+icon(r.itemId)+'<span class="lq-reward-name">'+name(r.itemId)+'<small>'+esc(optionSummary(r.options)||'Chỉ số mặc định của vật phẩm')+'</small></span><label class="lq-item-id">ID<input type="number" min="0" max="32767" data-lq-item="'+m.id+'" data-index="'+index+'" value="'+r.itemId+'" aria-label="ID quà mốc '+(m.id+1)+'"></label><input type="number" min="1" max="2147483647" data-lq-quantity="'+m.id+'" data-index="'+index+'" value="'+r.quantity+'" aria-label="Số lượng '+esc(item(r.itemId).name)+'"><button class="btn-mini" type="button" data-lq-options="'+m.id+'" data-index="'+index+'">Option ('+r.options.length+')</button><button class="btn-mini btn-mini--danger" type="button" data-lq-remove-reward="'+m.id+'" data-index="'+index+'">Xóa</button></div>';}).join('')+'<p class="field__hint">Bấm “Thêm vật phẩm” tại mốc này; sau khi thêm có thể chỉnh số lượng và option.</p></div>';}).join('');}
    function fold(value){return String(value).toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d');}
    function search(){
        if(!data)return;var q=fold(el('lqSearch').value.trim());
        var found=q?data.catalog.items.filter(function(i){return String(i.id)===q||fold(i.name).includes(q)||String(i.id).includes(q);}).sort(function(a,b){return (String(b.id)===q?1:0)-(String(a.id)===q?1:0);}).slice(0,20):[];
        el('lqResults').innerHTML=found.map(function(i){return '<div class="lq-search-row">'+icon(i.id)+'<button class="lq-pick" type="button" data-lq-pick="'+i.id+'">'+name(i.id)+'<br><small>Chọn vật phẩm</small></button></div>';}).join('')||(q?'<p>Không tìm thấy vật phẩm.</p>':'');
    }
    function pick(id){addItem(id,el('lqTarget').value,1);}
    function addItem(id,destination,quantity){
        if(busy||saving||loadError||!data||!data.catalog.items.some(function(i){return i.id===id;}))return false;var target=destination.split(':');
        if(target[0]==='price'){draft.values.idVatPhamQuay=id;el('lq_idVatPhamQuay').value=id;}
        else if(target[0]==='group'){var ids=draft.groups[target[1]];if(ids.includes(id)){toast('Nhóm đã có vật phẩm này.','error');return false;}if(ids.length>=200){toast('Mỗi nhóm tối đa 200 vật phẩm.','error');return false;}ids.push(id);groups();}
        else{var rewards=draft.milestones[Number(target[1])].rewards;if(rewards.length>=100){toast('Mỗi mốc tối đa 100 món quà.','error');return false;}if(!Number.isInteger(quantity)||quantity<1||quantity>2147483647){toast('Số lượng phải là số nguyên từ 1 đến 2147483647.','error');return false;}rewards.push({itemId:id,quantity:quantity,options:[]});milestones();}
        changed();return true;
    }
    function openPicker(target){
        if(busy||saving||loadError||!data)return;
        pickerTarget=target;var parts=target.split(':'),isGroup=parts[0]==='group';
        var label=isGroup?data.groupFields.find(function(g){return g.key===parts[1];}).label:'Quà mốc '+(Number(parts[1])+1);
        el('lqPickerTitle').textContent='Thêm vật phẩm — '+label;
        el('lqPickerSearch').value='';el('lqPickerQuantity').value='1';el('lqPickerQuantityField').hidden=isGroup;
        el('lqPickerStatus').textContent=isGroup?'Mỗi món được thêm một lần trong nhóm.':'Chọn số lượng rồi thêm quà. Có thể chỉnh option sau khi đóng cửa sổ.';
        pickerSearch();el('lqPickerDialog').showModal();el('lqPickerSearch').focus();
    }
    function pickerSearch(){
        if(!data||!pickerTarget)return;
        var q=fold(el('lqPickerSearch').value.trim()),parts=pickerTarget.split(':'),ids=parts[0]==='group'?draft.groups[parts[1]]:[];
        var found=data.catalog.items.filter(function(i){return !q||String(i.id).includes(q)||fold(i.name).includes(q);}).sort(function(a,b){return (String(b.id)===q?1:0)-(String(a.id)===q?1:0);}).slice(0,20);
        el('lqPickerResults').innerHTML=found.map(function(i){var exists=ids.includes(i.id);return '<div class="lq-search-row">'+icon(i.id)+'<button class="lq-pick" type="button" data-lq-picker-pick="'+i.id+'"'+(exists?' disabled':'')+'>'+name(i.id)+'<br><small>'+(exists?'Đã có trong nhóm':'+ Thêm vật phẩm')+'</small></button></div>';}).join('')||'<p>Không tìm thấy vật phẩm.</p>';
    }
    el('lqPickerSearch').oninput=pickerSearch;
    el('lqPickerResults').onclick=function(e){
        var b=e.target.closest('button');if(!b||busy||saving||loadError||!pickerTarget)return;
        if(b.dataset.lqIcon!==undefined){preview(Number(b.dataset.lqIcon));return;}
        if(b.dataset.lqPickerPick===undefined)return;
        var id=Number(b.dataset.lqPickerPick),quantity=Number(el('lqPickerQuantity').value);
        if(addItem(id,pickerTarget,quantity)){el('lqPickerStatus').textContent='Đã thêm '+item(id).name+'. Có thể thêm tiếp hoặc bấm “Xong”, rồi “Lưu & áp dụng”.';pickerSearch();}
    };
    el('lqPickerDialog').addEventListener('close',function(){pickerTarget=null;});
    function optionRow(option){
        var row=document.createElement('div');row.className='lq-option-row';row.innerHTML='<select aria-label="Option">'+data.catalog.options.map(function(o){return '<option value="'+o.id+'"'+(o.id===option.id?' selected':'')+'>'+o.id+' · '+esc(o.name)+'</option>';}).join('')+'</select><input type="number" min="0" max="65535" value="'+option.param+'" aria-label="Giá trị option" required><button class="btn-mini" type="button" aria-label="Xóa option">×</button>';row.querySelector('button').onclick=function(){row.remove();};el('lqOptionRows').appendChild(row);
    }
    function openOptions(m,index){editing={milestone:m,index:index};el('lqOptionTitle').textContent='Mốc '+(m+1)+' — '+item(draft.milestones[m].rewards[index].itemId).name;el('lqOptionRows').replaceChildren();draft.milestones[m].rewards[index].options.forEach(optionRow);el('lqOptionDialog').showModal();}
    function preview(id){
        var selected=item(id),dialog=el('lqIconDialog'),frame=el('lqIconFrame'),status=el('lqIconStatus');el('lqIconTitle').textContent=selected.name+' · #'+id;frame.replaceChildren();status.textContent='Đang tải icon…';dialog.showModal();
        if(selected.icon<0){status.textContent='Vật phẩm chưa có icon.';return;}
        var img=new Image(),scale=4;img.alt=selected.name;img.onload=function(){if(frame.firstElementChild===img){status.textContent='';img.hidden=false;}};img.onerror=function(){if(frame.firstElementChild!==img)return;if(--scale>0)img.src='/game-data/icon/x'+scale+'/'+selected.icon+'.png';else status.textContent='Không tải được icon.';};img.hidden=true;frame.appendChild(img);img.src='/game-data/icon/x4/'+selected.icon+'.png';
    }
    async function save(){
        if(busy||saving||!draft||loadError)return;
        for(var f of data.fields)if(!Number.isInteger(draft.values[f.key])||draft.values[f.key]<f.min||draft.values[f.key]>f.max){toast('Giá trị không hợp lệ: '+f.label,'error');return;}
        if(!global.confirm('Lưu cấu hình Vòng quay Thượng Đế và áp dụng cho các lượt quay tiếp theo?'))return;
        saving=true;lock(true);
        try{var result=await global.PanelAPI.luckyRound.save(Object.assign({revision:data.revision},clone(draft)));dirty=false;toast(result.message);if(active){busy=false;saving=false;await load(false);}else data=null;}
        catch(e){el('lqStatus').textContent=e.message;toast(e.message,'error');}
        finally{saving=false;if(active)lock(false);else busy=false;}
    }
    el('lqReload').onclick=function(){load(true);};el('lqSave').onclick=save;el('lqSearch').oninput=search;
    el('lqFields').addEventListener('input',function(e){if(!e.target.dataset.lqValue)return;draft.values[e.target.dataset.lqValue]=e.target.value===''?NaN:Number(e.target.value);changed();});
    el('lqMilestones').addEventListener('input',function(e){if(e.target.dataset.lqSpin!==undefined)draft.milestones[Number(e.target.dataset.lqSpin)].spins=Number(e.target.value);else if(e.target.dataset.lqItem!==undefined)draft.milestones[Number(e.target.dataset.lqItem)].rewards[Number(e.target.dataset.index)].itemId=e.target.value===''?-1:Number(e.target.value);else if(e.target.dataset.lqQuantity!==undefined)draft.milestones[Number(e.target.dataset.lqQuantity)].rewards[Number(e.target.dataset.index)].quantity=Number(e.target.value);else return;changed();});
    el('lqMilestones').addEventListener('change',function(e){if(e.target.dataset.lqItem!==undefined)milestones();});
    el('view-lucky-round').addEventListener('click',function(e){
        var b=e.target.closest('button');if(!b||busy)return;
        if(b.dataset.lqAdd)openPicker(b.dataset.lqAdd);
        if(b.dataset.lqIcon!==undefined)preview(Number(b.dataset.lqIcon));
        if(b.dataset.lqPick!==undefined)pick(Number(b.dataset.lqPick));
        if(b.dataset.lqRemoveGroup){draft.groups[b.dataset.lqRemoveGroup]=draft.groups[b.dataset.lqRemoveGroup].filter(function(id){return id!==Number(b.dataset.id);});groups();changed();}
        if(b.dataset.lqRemoveReward!==undefined){draft.milestones[Number(b.dataset.lqRemoveReward)].rewards.splice(Number(b.dataset.index),1);milestones();changed();}
        if(b.dataset.lqOptions!==undefined)openOptions(Number(b.dataset.lqOptions),Number(b.dataset.index));
    });
    el('lqOptionAdd').onclick=function(){optionRow({id:data.catalog.options[0].id,param:0});};
    el('lqOptionForm').onsubmit=function(e){e.preventDefault();var options=[],seen=new Set();for(var row of el('lqOptionRows').children){var id=Number(row.querySelector('select').value),param=Number(row.querySelector('input').value);if(seen.has(id)||!Number.isInteger(param)||param<0||param>65535){toast('Option bị trùng hoặc giá trị không hợp lệ.','error');return;}seen.add(id);options.push({id:id,param:param});}draft.milestones[editing.milestone].rewards[editing.index].options=options;el('lqOptionDialog').close();milestones();changed();};
    document.querySelectorAll('[data-lq-close]').forEach(function(b){b.onclick=function(){el(b.dataset.lqClose).close();};});
    el('view-lucky-round').addEventListener('error',function(e){if(e.target.tagName==='IMG')e.target.hidden=true;},true);
    el('lqPickerDialog').addEventListener('error',function(e){if(e.target.tagName==='IMG')e.target.hidden=true;},true);
    global.LuckyRoundPanel={start:function(){active=true;if(saving)return;if(data&&dirty){lock(false);return;}load(false);},stop:function(){active=false;sequence++;if(!saving)busy=false;el('lqOptionDialog').close();el('lqIconDialog').close();el('lqPickerDialog').close();pickerTarget=null;}};
})(window);
