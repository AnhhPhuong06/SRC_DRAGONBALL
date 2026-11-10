package network.io;

 


import data.DataGame;
import interfaces.ISession;
import network.session.MySession;

public class MyKeyHandler extends KeyHandler {

    @Override
    public void sendKey(ISession session) {
        super.sendKey(session);
        DataGame.sendDataImageVersion((MySession) session);
        DataGame.sendVersionRes((MySession) session);
    }

}
