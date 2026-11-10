(function(global){
    'use strict';
    var active=false,busy=false,saving=false,snapshot=null,timer=null,sequence=0;
    function el(id){return document.getElementById(id);}
    function esc(value){return String(value==null?'':value).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}
    function update(){
        var expired=snapshot&&!snapshot.completed&&Date.now()>=snapshot.expiresAt;
        el('dbResetReload').disabled=busy||saving;
        el('dbResetConfirmation').disabled=busy||saving||!snapshot||snapshot.completed||!snapshot.canReset;
        el('dbResetSubmit').disabled=busy||saving||!snapshot||snapshot.completed||!snapshot.canReset||expired||el('dbResetConfirmation').value!==snapshot.confirmation;
        el('dbResetExpiry').textContent=!snapshot||snapshot.completed?'':expired?'Xác nhận đã hết hạn. Tải lại số liệu để tiếp tục.':'Lượt xác nhận có hiệu lực trong 2 phút kể từ khi tải số liệu.';
    }
    async function load(){
        if(busy||saving)return;var request=++sequence;busy=true;snapshot=null;el('dbResetPreview').hidden=true;el('dbResetConfirmation').value='';el('dbResetStatus').textContent='Đang tải số liệu…';update();
        try{
            var data=await global.PanelAPI.databaseReset.info();if(!active||request!==sequence)return;snapshot=data;
            if(data.completed){el('dbResetStatus').textContent=data.message||'Đã reset dữ liệu. Cần khởi động lại server.';return;}
            el('dbResetPreview').hidden=false;el('dbResetDatabase').textContent=data.database;el('dbResetPhrase').textContent=data.confirmation;
            el('dbResetRows').innerHTML=data.tables.map(function(t){return '<tr><td>'+esc(t.name)+'</td><td>'+Number(t.rows).toLocaleString('vi-VN')+'</td></tr>';}).join('');
            el('dbResetBlockers').hidden=!data.blockers.length;el('dbResetBlockers').innerHTML=data.blockers.map(function(message){return '<p>'+esc(message)+'</p>';}).join('');
            el('dbResetStatus').textContent=data.canReset?'Kiểm tra database và số dòng trước khi nhập xác nhận xóa.':'Chưa thể reset. Xử lý các điều kiện bên dưới rồi tải lại.';
        }catch(e){if(active&&request===sequence){snapshot=null;el('dbResetStatus').textContent=e.message;global.Panel.toast(e.message,'error');}}
        finally{if(request===sequence){busy=false;update();}}
    }
    el('dbResetReload').onclick=load;el('dbResetConfirmation').oninput=update;
    el('dbResetForm').onsubmit=async function(e){
        e.preventDefault();update();if(el('dbResetSubmit').disabled)return;
        var body={token:snapshot.token,confirmation:el('dbResetConfirmation').value};saving=true;el('dbResetStatus').textContent='Đang reset database. Chờ xử lý hoàn tất…';update();
        try{
            var result=await global.PanelAPI.databaseReset.reset(body);snapshot=result;el('dbResetPreview').hidden=true;el('dbResetStatus').textContent=result.message;global.Panel.toast(result.message);
        }catch(error){snapshot=null;el('dbResetStatus').textContent=error.message;el('dbResetConfirmation').value='';global.Panel.toast(error.message,'error');}
        finally{saving=false;update();if(active&&!timer)timer=global.setInterval(update,1000);}
    };
    global.DatabaseResetPanel={start:function(){active=true;if(!timer)timer=global.setInterval(update,1000);if(!saving)load();},stop:function(){active=false;sequence++;if(!saving)busy=false;if(timer){global.clearInterval(timer);timer=null;}el('dbResetConfirmation').value='';}};
})(window);
