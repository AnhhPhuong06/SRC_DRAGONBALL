(function (global) {
    'use strict';
    var data=null,selected=null,active=false,busy=false,sequence=0,currentTab='ranking';
    function el(id){return document.getElementById(id);}
    function esc(v){return String(v==null?'':v).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');}
    function notice(text,kind){global.Panel.toast(text,kind||'success');}
    function canEdit(){return selected&&selected.supported&&!selected.error&&!selected.sending&&!selected.recipients.length&&(!selected.done||el('tfNewCycle').checked)&&!(selected.auto&&selected.due);}
    function lock(value){
        busy=value;el('topReload').disabled=value;el('topSelect').disabled=value||!data;
        el('topConfigFields').disabled=value||!canEdit();el('topSave').disabled=value||!canEdit();
        el('tfNewCycle').disabled=value||!selected||!selected.done||!!selected.recipients.length;
        el('topToggle').disabled=value||!selected||!selected.supported||selected.done||selected.sending;
        el('topAward').disabled=value||!selected||!selected.supported||!selected.runtime||!selected.auto||!selected.due||selected.done||selected.sending||!!selected.error;
    }
    function tab(name){currentTab=name;['ranking','rewards','progress'].forEach(function(t){el('top'+t[0].toUpperCase()+t.slice(1)+'Tab').hidden=t!==name;});document.querySelectorAll('[data-top-tab]').forEach(function(b){b.classList.toggle('btn--primary',b.dataset.topTab===name);});}
    async function load(){
        if(busy)return;var request=++sequence;lock(true);
        try{
            var next=await global.PanelAPI.top.info();if(!active||request!==sequence)return;
            var old=selected?selected.id:Number(el('topSelect').value);data=next;
            el('topNotes').textContent=data.notes;el('topClock').textContent='Thời gian server: '+data.serverTime.replace('T',' ')+' · '+data.timezone;
            el('topTotal').textContent=data.tops.length;el('topAutoCount').textContent=data.tops.filter(function(t){return t.auto&&!t.done;}).length;el('topDoneCount').textContent=data.tops.filter(function(t){return t.done;}).length;
            el('topSelect').innerHTML=data.tops.map(function(t){return '<option value="'+t.id+'">#'+t.id+' · '+esc(t.name)+'</option>';}).join('');
            el('topItemCatalog').innerHTML=data.catalog.items.map(function(i){return '<option value="'+i.id+'">'+esc(i.name)+'</option>';}).join('');
            el('topOptionCatalog').innerHTML=data.catalog.options.map(function(i){return '<option value="'+i.id+'">'+esc(i.name)+'</option>';}).join('');
            var extra=el('topExtraSelect').value;el('topExtraCard').hidden=!(data.extraBoards||[]).length;
            el('topExtraSelect').innerHTML=(data.extraBoards||[]).map(function(b){return '<option value="'+esc(b.key)+'">'+esc(b.name)+'</option>';}).join('');
            if((data.extraBoards||[]).some(function(b){return b.key===extra;}))el('topExtraSelect').value=extra;renderExtra();
            selected=data.tops.find(function(t){return t.id===old;})||data.tops[0]||null;
            el('topError').hidden=true;render();
        }catch(error){if(active&&request===sequence){data=null;selected=null;el('topDetail').hidden=true;el('topExtraCard').hidden=true;el('topError').textContent='Không tải được top: '+error.message;el('topError').hidden=false;notice(error.message,'error');}}
        finally{if(request===sequence)lock(false);}
    }
    function render(){
        el('topDetail').hidden=!selected;if(!selected)return;
        el('topSelect').value=selected.id;el('topTitle').textContent='TOP '+selected.name;
        el('topStatus').textContent=selected.done?'Đã hoàn tất':selected.sending?'Đang trao':selected.auto?(selected.due?'Đến hạn trao':'Đã lên lịch'):'Đang tạm dừng';
        el('topToggle').textContent=selected.auto?'Tạm dừng tự động':'Bật trao tự động';
        el('topSummary').textContent='Ngày trao: '+(selected.date||'Chưa đặt').replace('T',' ')+' · '+selected.limit+' hạng nhận quà · '+selected.recipients.length+' người đã nhận trong đợt';
        el('topError').textContent=selected.error;el('topError').hidden=!selected.error;
        el('topBoardError').textContent=selected.boardError;el('topBoardError').hidden=!selected.boardError;
        var received=new Set(selected.recipients.map(function(u){return u.id;}));
        el('topRankingBody').innerHTML=selected.ranking.map(function(r){return '<tr><td>'+r.rank+'</td><td>'+r.id+'</td><td><b>'+esc(r.name||('#'+r.id))+'</b></td><td>'+esc(/^\d+$/.test(r.score)?Number(r.score).toLocaleString('vi-VN'):r.score)+'</td><td>'+(selected.done?'Đợt đã hoàn tất':received.has(r.id)?'Đã nhận':r.rank<=selected.limit?'Trong hạng nhận quà':'Ngoài hạng nhận quà')+'</td><td>'+esc(r.source)+'</td></tr>';}).join('');
        el('topRankingEmpty').hidden=selected.ranking.length>0;
        el('topProgressBody').innerHTML=selected.recipients.map(function(u){return '<tr><td>'+u.id+'</td><td>'+u.top+'</td></tr>';}).join('');el('topProgressEmpty').hidden=selected.recipients.length>0;
        el('tfName').value=selected.name;el('tfDate').value=selected.date;el('tfLimit').value=selected.limit;el('tfNewCycle').checked=false;el('topNewCycleWrap').hidden=!selected.done;
        el('topEditHint').textContent=selected.recipients.length?'Đợt đã có người nhận: chỉ bật/tạm dừng tự động, giữ nguyên quà và tiến độ.':selected.auto&&selected.due?'Top đã đến hạn đang bật tự động. Tạm dừng trước khi sửa quà hoặc lịch.':selected.done?'Đợt đã hoàn tất. Chọn mở đợt mới, nhập ngày tương lai và kiểm tra quà; sau khi lưu bạn bật tự động riêng.':'Lưu sẽ áp dụng quà và lịch trong game, giữ nguyên danh sách đã nhận.';
        el('topRewardGroups').innerHTML='';selected.rewards.forEach(addRank);tab(currentTab);lock(busy);
    }
    function renderExtra(){
        if(!data)return;var board=(data.extraBoards||[]).find(function(b){return b.key===el('topExtraSelect').value;});if(!board)return;
        el('topExtraError').textContent=board.error;el('topExtraError').hidden=!board.error;
        el('topExtraBody').innerHTML=board.ranking.map(function(r){return '<tr><td>'+r.rank+'</td><td>'+r.id+'</td><td>'+esc(r.name)+'</td><td>'+esc(/^\d+$/.test(r.score)?Number(r.score).toLocaleString('vi-VN'):r.score)+'</td></tr>';}).join('');
        el('topExtraEmpty').hidden=board.ranking.length>0;
    }
    function optionHtml(o){return '<div class="top-option"><input type="text" data-top-option-id list="topOptionCatalog" pattern="[0-9]+" required aria-label="ID option" placeholder="ID option" value="'+esc(o.id)+'"><input type="number" data-top-option-param min="0" max="65535" step="1" required aria-label="Tham số option" value="'+esc(o.param)+'"><button class="btn-mini" type="button" data-top-remove-option>Xóa</button><span class="field__hint top-option-name" data-top-option-name></span></div>';}
    function optionName(row){var raw=row.querySelector('[data-top-option-id]').value;var option=data.catalog.options.find(function(o){return raw.trim()!==''&&o.id===Number(raw);});row.querySelector('[data-top-option-name]').textContent=option?option.name.replace(/#/g,row.querySelector('[data-top-option-param]').value):'Chọn ID option và nhập tham số.';}
    function rankSummary(group){if(!group)return;group.querySelector('summary').textContent='TOP '+group.querySelector('[data-top-rank]').value+' · '+group.querySelector('[data-top-items]').children.length+' vật phẩm';}
    function addItem(container,item){
        item=item||{id:'',quantity:1,options:[]};if(container.children.length>=20)return notice('Tối đa 20 vật phẩm mỗi hạng.','error');
        var row=document.createElement('div');row.className='top-reward-item';row.innerHTML='<div class="top-item-fields"><div class="field"><label>ID VẬT PHẨM</label><input type="text" data-top-item-id list="topItemCatalog" pattern="[0-9]+" required value="'+esc(item.id)+'"></div><div class="field"><label>SỐ LƯỢNG</label><input type="number" data-top-item-quantity min="1" max="99999" step="1" required value="'+esc(item.quantity)+'"></div><button class="btn" type="button" data-top-remove-item>Xóa vật phẩm</button></div><p class="field__hint" data-top-item-name></p><div data-top-options>'+item.options.map(optionHtml).join('')+'</div><button class="btn-mini" type="button" data-top-add-option>Thêm option</button>';
        var info=row.querySelector('[data-top-item-name]');info.classList.add('top-item-info');
        var thumb=document.createElement('span');thumb.className='top-item-thumb';thumb.setAttribute('data-top-item-thumb','');thumb.setAttribute('aria-hidden','true');
        var name=document.createElement('span');name.setAttribute('data-top-item-label','');info.appendChild(thumb);info.appendChild(name);
        container.appendChild(row);itemName(row);row.querySelectorAll('.top-option').forEach(optionName);rankSummary(container.closest('.top-rank'));
    }
    function itemName(row){
        var raw=row.querySelector('[data-top-item-id]').value.trim(),item=data.catalog.items.find(function(i){return raw!==''&&i.id===Number(raw);});
        row.querySelector('[data-top-item-label]').textContent=item?item.name:'Chọn ID trong danh sách vật phẩm của server.';
        var thumb=row.querySelector('[data-top-item-thumb]');thumb.textContent='—';
        if(item&&Number.isInteger(item.icon)&&item.icon>=0&&item.icon<=999999){
            var img=document.createElement('img');img.alt='';img.loading='lazy';img.src='/game-data/icon/x1/'+item.icon+'.png';
            img.onerror=function(){if(img.parentNode===thumb)thumb.textContent='—';};thumb.replaceChildren(img);
        }
    }
    function addRank(group){
        group=group||{top:1,items:[]};if(el('topRewardGroups').children.length>=100)return notice('Tối đa 100 hạng.','error');
        var row=document.createElement('details');row.className='top-rank';row.open=el('topRewardGroups').children.length===0;
        row.innerHTML='<summary>TOP '+esc(group.top)+' · '+group.items.length+' vật phẩm</summary><div class="top-rank-body"><div class="top-rank-toolbar"><div class="field"><label>HẠNG NHẬN QUÀ</label><input type="number" data-top-rank min="1" max="100" step="1" required value="'+esc(group.top)+'"></div><button class="btn" type="button" data-top-add-item>Thêm vật phẩm</button><button class="btn" type="button" data-top-remove-rank>Xóa hạng</button></div><div data-top-items></div></div>';
        el('topRewardGroups').appendChild(row);group.items.forEach(function(i){addItem(row.querySelector('[data-top-items]'),i);});
    }
    function payload(){
        var seen=new Set();return Array.from(el('topRewardGroups').children).map(function(group){
            var rank=Number(group.querySelector('[data-top-rank]').value);if(seen.has(rank))throw new Error('Trùng hạng nhận quà: '+rank);seen.add(rank);
            return {top:rank,items:Array.from(group.querySelector('[data-top-items]').children).map(function(row){
                var options=Array.from(row.querySelector('[data-top-options]').children).map(function(o){return {id:Number(o.querySelector('[data-top-option-id]').value),param:Number(o.querySelector('[data-top-option-param]').value)};});
                if(new Set(options.map(function(o){return o.id;})).size!==options.length)throw new Error('Một vật phẩm không được trùng ID option.');
                return {id:Number(row.querySelector('[data-top-item-id]').value),quantity:Number(row.querySelector('[data-top-item-quantity]').value),options:options};
            })};
        });
    }
    async function mutate(action,body){
        if(busy||!selected)return;var request=++sequence;lock(true);
        try{var response=await global.PanelAPI.top.run(action,body);if(!active||request!==sequence)return;notice(response.message);lock(false);await load();}
        catch(error){if(active&&request===sequence)notice(error.message,'error');}finally{if(request===sequence)lock(false);}
    }
    el('topForm').addEventListener('submit',function(event){
        event.preventDefault();if(busy||!canEdit()||!el('topForm').reportValidity())return;
        try{var action=el('tfNewCycle').checked?'new-cycle':'save';if(action==='new-cycle'&&!global.confirm('Mở đợt top mới với quà và lịch đang nhập? Đợt mới sẽ tắt tự động để bạn kiểm tra.'))return;
            mutate(action,{id:selected.id,version:selected.version,name:el('tfName').value.trim(),date:el('tfDate').value,limit:Number(el('tfLimit').value),rewards:payload()});
        }catch(error){notice(error.message,'error');}
    });
    el('topToggle').addEventListener('click',function(){if(!busy&&selected&&global.confirm((selected.auto?'Tạm dừng':'Bật')+' trao thưởng tự động cho Top '+selected.name+'?'))mutate('toggle',{id:selected.id,version:selected.version,enabled:!selected.auto});});
    el('topAward').addEventListener('click',function(){if(!busy&&selected&&global.confirm('Trao quà Top '+selected.name+' đã đến hạn cho những người chưa nhận qua hòm thư?'))mutate('award',{id:selected.id,version:selected.version});});
    el('topSelect').addEventListener('change',function(){if(!busy&&data){var id=Number(this.value);selected=data.tops.find(function(t){return t.id===id;});render();}});
    el('topReload').addEventListener('click',function(){if(!busy)load();});el('tfNewCycle').addEventListener('change',function(){lock(busy);});
    el('topExtraSelect').addEventListener('change',renderExtra);
    el('topRankAdd').addEventListener('click',function(){if(!busy&&canEdit()){var used=new Set(Array.from(el('topRewardGroups').children).map(function(g){return Number(g.querySelector('[data-top-rank]').value);}));var rank=1;while(used.has(rank)&&rank<=100)rank++;addRank({top:rank,items:[]});if(el('topRewardGroups').lastElementChild)el('topRewardGroups').lastElementChild.open=true;}});
    el('view-top-gift').addEventListener('click',function(event){
        var button=event.target.closest('button');if(!button)return;if(button.dataset.topTab)return tab(button.dataset.topTab);
        if(busy||!canEdit())return;var group=button.closest('.top-rank'),row=button.closest('.top-reward-item');
        if(button.hasAttribute('data-top-add-item'))addItem(group.querySelector('[data-top-items]'));
        if(button.hasAttribute('data-top-remove-item')){row.remove();rankSummary(group);}if(button.hasAttribute('data-top-remove-rank'))group.remove();
        if(button.hasAttribute('data-top-add-option')){var container=row.querySelector('[data-top-options]');if(container.children.length>=32)return notice('Tối đa 32 option mỗi vật phẩm.','error');container.insertAdjacentHTML('beforeend',optionHtml({id:'',param:0}));optionName(container.lastElementChild);}
        if(button.hasAttribute('data-top-remove-option'))button.closest('.top-option').remove();
    });
    el('topRewardGroups').addEventListener('input',function(event){var row=event.target.closest('.top-reward-item');if(row&&event.target.hasAttribute('data-top-item-id'))itemName(row);var option=event.target.closest('.top-option');if(option)optionName(option);if(event.target.hasAttribute('data-top-rank'))rankSummary(event.target.closest('.top-rank'));});
    global.TopPanel={start:function(){active=true;document.body.classList.add('top-view');load();},stop:function(){active=false;sequence++;busy=false;document.body.classList.remove('top-view');}};
})(window);
