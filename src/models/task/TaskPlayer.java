package models.task;

 
public class TaskPlayer {

    public TaskMain taskMain;

    public SideTask sideTask;

    public ClanTask clanTask;

    public TaskDanhHieu taskdh;
    
    public Task_Kol task_kol;

    public TaskPlayer() {
        this.sideTask = new SideTask();
        this.clanTask = new ClanTask();
        this.taskdh = new TaskDanhHieu();
        this.task_kol = new Task_Kol();
    }

    public void dispose() {
        this.taskMain = null;
        this.sideTask = null;
        this.clanTask = null;
        this.taskdh = null;
        this.task_kol = null;
    }

}
