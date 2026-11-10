(function(global){
    'use strict';
    var data=null,active=false,busy=false,sequence=0,editId=0,editRevision=0,itemSearchRows=[];
    function el(id){return document.getElementById(id);}
    function esc(v){return String(v==null?'':v).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}
    function notice(message,kind){global.Panel.toast(message,kind||'success');}
    function lock(value){
        busy=value;el('bdropReload').disabled=value;
        ['bdropAdd','bdropRuleSave','bdropOptionAdd'].forEach(function(id){el(id).disabled=value||!data||!!data.loadError;});
        el('bdropRuleForm').querySelectorAll('input,select,button').forEach(function(input){input.disabled=value||!data||!!data.loadError;});
        el('bdropBody').querySelectorAll('button').forEach(function(button){button.disabled=value||!data||!!data.loadError;});
    }
    function icon(id){return Number.isInteger(id)&&id>=0&&id<=999999?'<img src="/game-data/icon/x1/'+id+'.png" alt="" loading="lazy">':'';}
    function iconButton(item){return '<button type="button" class="bdrop-icon-trigger" data-bdrop-preview="'+item.id+'" title="Xem icon '+esc(item.name)+'" aria-label="Xem icon '+esc(item.name)+'"><span class="bdrop-choice-thumb" aria-hidden="true">'+icon(item.icon)+'</span></button>';}
    function previewItem(id){
        if(!data||busy)return;
        var item=data.catalog.items.find(function(i){return i.id===id;});
        if(!item){var rule=data.rules.find(function(r){return r.itemId===id;});if(rule)item={id:rule.itemId,name:rule.itemName,icon:rule.icon};}if(!item)return;
        var dialog=el('bdropIconDialog'),frame=el('bdropIconFrame'),status=el('bdropIconStatus');
        el('bdropIconTitle').textContent=item.name;el('bdropIconItemId').textContent='ID vật phẩm: '+item.id;
        frame.replaceChildren();status.textContent='Đang tải icon…';status.hidden=false;
        if(!dialog.open)dialog.showModal();
        if(!Number.isInteger(item.icon)||item.icon<0||item.icon>999999){status.textContent='Vật phẩm này chưa có ảnh icon.';return;}
        var img=document.createElement('img'),scale=4;img.id='bdropIconImage';img.alt='Icon '+item.name;img.hidden=true;
        img.onload=function(){if(frame.firstElementChild===img&&dialog.open){img.hidden=false;status.hidden=true;}};
        img.onerror=function(){
            if(frame.firstElementChild!==img||!dialog.open)return;
            if(--scale>0)img.src='/game-data/icon/x'+scale+'/'+item.icon+'.png';
            else{img.remove();status.textContent='Vật phẩm này chưa có ảnh icon.';}
        };
        frame.appendChild(img);img.src='/game-data/icon/x4/'+item.icon+'.png';
    }
    function searchText(value){return String(value).toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d');}
    function closeItemSearch(){el('bdropItemResults').hidden=true;el('bdfItemSearch').setAttribute('aria-expanded','false');}
    function searchItems(){
        if(busy||!data||data.loadError)return;
        var raw=el('bdfItemSearch').value.trim(),words=searchText(raw).split(/\s+/).filter(Boolean);
        var matches=itemSearchRows.filter(function(row){return words.every(function(word){return row.text.includes(word);});});
        if(/^\d+$/.test(raw))matches.sort(function(a,b){return Number(b.item.id===Number(raw))-Number(a.item.id===Number(raw));});
        el('bdropItemCount').textContent=matches.length?'Có '+matches.length+' vật phẩm'+(matches.length>20?' · Hiển thị 20 món đầu, nhập thêm để thu hẹp.':'. Chọn tên để điền ID; bấm icon để xem.'):'Không tìm thấy vật phẩm. Thử tên hoặc ID khác.';
        el('bdropItemChoices').innerHTML=matches.slice(0,20).map(function(row){var item=row.item;return '<div class="bdrop-item-result">'+iconButton(item)+'<button type="button" class="bdrop-item-choice" data-bdrop-item="'+item.id+'"><span class="bdrop-choice-text"><b>'+esc(item.name)+'</b><small>ID: '+item.id+'</small></span></button></div>';}).join('');
        el('bdropItemResults').hidden=false;el('bdfItemSearch').setAttribute('aria-expanded','true');
    }
    function selectItem(button){
        if(busy||!data||data.loadError||!button)return;
        var item=data.catalog.items.find(function(i){return i.id===Number(button.dataset.bdropItem);});if(!item)return;
        el('bdfItem').value=item.id;el('bdfItemSearch').value=item.name;itemName();closeItemSearch();el('bdfItem').focus();
    }
    function render(){
        var needle=el('bdropSearch').value.trim().toLowerCase();
        var rows=data.rules.filter(function(r){return [r.name,r.id,r.itemId,r.itemName,r.mapName,r.bossKey,r.bossName].join(' ').toLowerCase().includes(needle);});
        // Gộp theo boss: cột boss chỉ hiển thị 1 lần (rowspan), mỗi nguyên liệu 1 dòng gọn
        var groups={};
        rows.forEach(function(r){(groups[r.bossKey]=groups[r.bossKey]||[]).push(r);});
        var html=Object.keys(groups).map(function(key){
            var g=groups[key].slice().sort(function(a,b){return a.itemId-b.itemId||a.id-b.id;});
            var bossCell='<td rowspan="'+g.length+'" class="bdrop-boss-cell"><b>'+esc(g[0].bossName)+'</b><br><small>'+esc(g[0].bossKey)+'</small></td>';
            return g.map(function(r,i){
                var first=(i===0?bossCell:'');
                return '<tr>'+first+'<td>'+(r.enabled?'\u0110ang b\u1eadt':'\u0110ang t\u1eaft')+'</td><td><small>#'+r.id+'</small></td><td><span class="bdrop-item">'+iconButton({id:r.itemId,name:r.itemName,icon:r.icon})+esc(r.itemName)+' (#'+r.itemId+')</span></td><td>'+esc(r.mapName)+'</td><td>'+r.chance+'%</td><td>'+r.minQuantity+(r.maxQuantity===r.minQuantity?'':'\u2013'+r.maxQuantity)+'</td><td>'+r.options.map(function(o){var def=data.catalog.options.find(function(d){return d.id===o.id;});return esc(def?def.name.replace(/#/g,o.param):'#'+o.id+': '+o.param);}).join('<br>')+'</td><td class="drop-actions"><button class="btn-mini" type="button" data-bdrop-edit="'+r.id+'">S\u1eeda</button> <button class="btn-mini" type="button" data-bdrop-toggle="'+r.id+'">'+(r.enabled?'T\u1eaft':'B\u1eadt')+'</button> <button class="btn-mini" type="button" data-bdrop-delete="'+r.id+'">X\u00f3a</button></td></tr>';
            }).join('');
        }).join('');
        el('bdropBody').innerHTML=html;el('bdropEmpty').hidden=rows.length>0;el('bdropEmpty').textContent=needle?'Không có drop boss khớp tìm kiếm.':'Chưa có drop boss tùy chỉnh. Chọn “Thêm drop boss” để tạo.';
    }
    async function load(){
        if(busy)return;var request=++sequence;lock(true);
        try{
            var next=await global.PanelAPI.bossDrop.info();if(!active||request!==sequence)return;data=next;
            itemSearchRows=data.catalog.items.map(function(item){return {item:item,text:searchText(item.id+' '+item.name)};});closeItemSearch();
            el('bdropTotal').textContent=data.rules.length+'/'+data.maxRules;el('bdropEnabled').textContent=data.rules.filter(function(r){return r.enabled;}).length;
            el('bdropRateCount').textContent=data.catalog.bosses.length;el('bdropNotes').textContent=data.notes;
            el('bdropError').textContent=data.loadError||'';el('bdropError').hidden=!data.loadError;
            el('bdfMap').innerHTML='<option value="-1">Tất cả map</option>'+data.catalog.maps.map(function(m){return '<option value="'+m.id+'">#'+m.id+' · '+esc(m.name)+'</option>';}).join('');
            el('bdfBoss').innerHTML=data.catalog.bosses.map(function(b){return '<option value="'+esc(b.key)+'">'+esc(b.name)+' · '+esc(b.key)+'</option>';}).join('');
            [['bdropItems',data.catalog.items],['bdropOptionCatalog',data.catalog.options]].forEach(function(pair){el(pair[0]).innerHTML=pair[1].map(function(i){return '<option value="'+i.id+'">'+esc(i.name)+'</option>';}).join('');});render();
        }catch(error){if(active&&request===sequence){data=null;el('bdropEditor').hidden=true;el('bdropBody').innerHTML='';el('bdropError').textContent='Không tải được drop boss: '+error.message;el('bdropError').hidden=false;notice(error.message,'error');}}
        finally{if(request===sequence)lock(false);}
    }
    function itemName(){var raw=el('bdfItem').value.trim(),item=data&&data.catalog.items.find(function(i){return raw!==''&&i.id===Number(raw);});el('bdfItemName').innerHTML=item?iconButton(item)+'<span>'+esc(item.name)+'</span>':'Chọn ID vật phẩm trong danh sách.';}
    function optionName(row){var raw=row.querySelector('[data-bdrop-option-id]').value.trim(),def=data.catalog.options.find(function(o){return raw!==''&&o.id===Number(raw);});row.querySelector('.bdrop-option-label').textContent=def?def.name.replace(/#/g,row.querySelector('[data-bdrop-option-param]').value):'Chọn option trong danh sách.';}
    function addOption(option){
        if(el('bdropOptions').children.length>=32)return notice('Tối đa 32 option.','error');option=option||{id:'',param:0};var row=document.createElement('div');row.className='drop-option-row';
        row.innerHTML='<div class="field"><label>ID OPTION</label><input type="text" data-bdrop-option-id list="bdropOptionCatalog" pattern="[0-9]+" required aria-label="ID option" value="'+esc(option.id)+'"></div><div class="field"><label>THAM SỐ</label><input data-bdrop-option-param type="number" min="0" max="65535" step="1" required aria-label="Tham số option" value="'+esc(option.param)+'"></div><button type="button" class="btn" data-bdrop-remove-option>Xóa option</button><span class="field__hint bdrop-option-label"></span>';
        el('bdropOptions').appendChild(row);optionName(row);
    }
    function open(rule){
        if(busy||!data||data.loadError)return;rule=rule||{id:0,name:'',enabled:true,bossKey:el('bdfBoss').options.length?el('bdfBoss').options[0].value:'',mapId:-1,itemId:'',chance:1,minQuantity:1,maxQuantity:1,options:[]};
        editId=rule.id;editRevision=data.revision;
        el('bdropEditorTitle').textContent=rule.id?'SỬA DROP BOSS #'+rule.id:'THÊM DROP BOSS';
        [['bdfName',rule.name],['bdfMap',rule.mapId],['bdfBoss',rule.bossKey],['bdfItem',rule.itemId],['bdfChance',rule.chance],['bdfMin',rule.minQuantity],['bdfMax',rule.maxQuantity],['bdfEnabled',rule.enabled?'1':'0']].forEach(function(pair){el(pair[0]).value=pair[1];});
        el('bdropOptions').innerHTML='';rule.options.forEach(addOption);el('bdfItemSearch').value='';closeItemSearch();itemName();el('bdropEditor').hidden=false;el('bdfName').focus();
    }
    async function mutate(action,body){
        if(busy||!data)return;var request=++sequence;lock(true);
        try{var response=await global.PanelAPI.bossDrop.run(action,body);if(!active||request!==sequence)return;notice(response.message);el('bdropEditor').hidden=true;lock(false);await load();}
        catch(error){if(active&&request===sequence)notice(error.message,'error');}finally{if(request===sequence)lock(false);}
    }
    el('bdropRuleForm').addEventListener('submit',function(event){
        event.preventDefault();if(busy||!data||data.loadError||!el('bdropRuleForm').reportValidity())return;
        var options=Array.from(el('bdropOptions').children).map(function(row){return {id:Number(row.querySelector('[data-bdrop-option-id]').value),param:Number(row.querySelector('[data-bdrop-option-param]').value)};});
        if(new Set(options.map(function(o){return o.id;})).size!==options.length)return notice('Không thêm trùng ID option.','error');
        var min=Number(el('bdfMin').value),max=Number(el('bdfMax').value);if(max<min)return notice('Số lượng nhiều nhất phải lớn hơn hoặc bằng ít nhất.','error');
        mutate('rule-save',{id:editId,revision:editRevision,name:el('bdfName').value.trim(),mapId:Number(el('bdfMap').value),bossKey:el('bdfBoss').value,itemId:Number(el('bdfItem').value),enabled:el('bdfEnabled').value==='1',chance:Number(el('bdfChance').value),minQuantity:min,maxQuantity:max,options:options});
    });
    el('view-boss-drop').addEventListener('click',function(event){
        var preview=event.target.closest('[data-bdrop-preview]');if(preview){previewItem(Number(preview.dataset.bdropPreview));return;}
        var button=event.target.closest('button');if(!button||busy||!data||data.loadError)return;
        if(button.hasAttribute('data-bdrop-remove-option'))button.closest('.drop-option-row').remove();
        var id=Number(button.dataset.bdropEdit||button.dataset.bdropToggle||button.dataset.bdropDelete),rule=data.rules.find(function(r){return r.id===id;});if(!rule)return;
        if(button.hasAttribute('data-bdrop-edit'))open(rule);
        if(button.hasAttribute('data-bdrop-toggle'))mutate('rule-save',Object.assign({},rule,{enabled:!rule.enabled,revision:data.revision}));
        if(button.hasAttribute('data-bdrop-delete')&&global.confirm('Xóa drop boss “'+rule.name+'”?'))mutate('rule-delete',{id:rule.id,revision:data.revision});
    });
    el('bdropOptions').addEventListener('input',function(event){var row=event.target.closest('.drop-option-row');if(row&&data)optionName(row);});
    el('view-boss-drop').addEventListener('error',function(event){if(event.target.tagName==='IMG')event.target.remove();},true);
    el('bdropAdd').addEventListener('click',function(){open();});el('bdropCancel').addEventListener('click',function(){if(!busy)el('bdropEditor').hidden=true;});
    el('bdropReload').addEventListener('click',function(){if(!busy){el('bdropEditor').hidden=true;load();}});el('bdropSearch').addEventListener('input',function(){if(data)render();});
    el('bdropOptionAdd').addEventListener('click',function(){if(!busy&&data&&!data.loadError)addOption();});el('bdfItem').addEventListener('input',function(){itemName();el('bdfItemSearch').value='';closeItemSearch();});
    el('bdfItemSearch').addEventListener('input',searchItems);el('bdfItemSearch').addEventListener('focus',searchItems);
    el('bdfItemSearch').addEventListener('keydown',function(event){
        if(event.key==='Enter'){event.preventDefault();if(!el('bdropItemResults').hidden)selectItem(el('bdropItemChoices').querySelector('[data-bdrop-item]'));}
        if(event.key==='ArrowDown'){event.preventDefault();searchItems();var first=el('bdropItemChoices').querySelector('[data-bdrop-item]');if(first)first.focus();}
        if(event.key==='Escape'){event.preventDefault();closeItemSearch();}
    });
    el('bdropItemChoices').addEventListener('click',function(event){selectItem(event.target.closest('[data-bdrop-item]'));});
    el('bdropItemChoices').addEventListener('keydown',function(event){
        var button=event.target.closest('[data-bdrop-item]');if(!button)return;
        if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();var buttons=Array.from(el('bdropItemChoices').querySelectorAll('[data-bdrop-item]')),next=buttons[buttons.indexOf(button)+(event.key==='ArrowDown'?1:-1)];if(next)next.focus();else if(event.key==='ArrowUp')el('bdfItemSearch').focus();}
        if(event.key==='Escape'){event.preventDefault();el('bdfItemSearch').focus();closeItemSearch();}
    });
    document.addEventListener('click',function(event){if(!el('bdropItemPicker').contains(event.target)&&!el('bdropIconDialog').contains(event.target))closeItemSearch();});
    el('bdropItemPicker').addEventListener('focusout',function(event){if(!el('bdropIconDialog').open&&event.relatedTarget&&!this.contains(event.relatedTarget))closeItemSearch();});
    el('bdropIconClose').addEventListener('click',function(){el('bdropIconDialog').close();});
    el('bdropIconDialog').addEventListener('click',function(event){var box=this.getBoundingClientRect();if(event.target===this&&(event.clientX<box.left||event.clientX>box.right||event.clientY<box.top||event.clientY>box.bottom))this.close();});
    global.BossDropPanel={start:function(){active=true;document.body.classList.add('drop-view');load();},stop:function(){active=false;sequence++;busy=false;el('bdropEditor').hidden=true;closeItemSearch();el('bdropIconDialog').close();if(location.hash!=='#drop')document.body.classList.remove('drop-view');}};
})(window);
