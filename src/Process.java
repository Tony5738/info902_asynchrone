import com.google.common.eventbus.Subscribe;


public class Process  implements Runnable {
    private Thread thread;
    private EventBusService bus;
    private boolean alive;
    private boolean dead;
    private int clock = 0;
    private String stateToken = "null";
    private static int nbProcess = 0;
    private int id = Process.nbProcess++;

    public Process(String name){

        this.bus = EventBusService.getInstance();
        this.bus.registerSubscriber(this); // Auto enregistrement sur le bus afin que les methodes "@Subscribe" soient invoquees automatiquement.
        this.thread = new Thread(this);
        this.thread.setName(name);
        this.alive = true;
        this.dead = false;
        this.thread.start();

    }

    @Subscribe
    public void onToken(Token t){

        if (t.getPayload().equals(this.id)) {

            if (stateToken.equals("request")) {
                stateToken = "sc";
                System.out.println(this.thread.getName() + " get critical section token");

                while (stateToken.equals("sc")) {
                    try {
                        Thread.sleep(50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                t.setPayload((this.id + 1) % Process.nbProcess);
                bus.postEvent(t);
                stateToken = "null";

            }else{

                t.setPayload((this.id + 1) % Process.nbProcess);
                bus.postEvent(t);
            }
        }

    }

    public void request(){
        stateToken = "request";

        while(!stateToken.equals("sc")){
            try{
                Thread.sleep(50);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public void release(){
        stateToken = "release";
    }



    public void broadcast(Object o)
    {
        this.clock++;
        System.out.println(Thread.currentThread().getName() + " clock : " + this.clock);
        AbstractMessage m1 = new BroadcastMessage(o,this.clock, Thread.currentThread().getName());
        System.out.println(Thread.currentThread().getName() + " send : " + m1.getPayload());
        bus.postEvent(m1);
    }

    // Declaration de la methode de callback invoquee lorsqu'un message de type AbstractMessage transite sur le bus
    @Subscribe
    public void onBroadcast(BroadcastMessage m){
        //receive
        if(!m.getSender().equals(this.thread.getName())){
            System.out.println(Thread.currentThread().getName() + " receives: " + m.getPayload() + " for " + this.thread.getName());
            if(m.getClock() > this.clock)
            {
                this.clock = m.getClock()+1;
            }
            else
            {
                this.clock++;
            }
        }

    }


    public void sendTo(Object o, int to) {
        this.clock++;
        System.out.println(this.thread.getName() + " send [" + o + "] to [id " + to + "], with clock at " + this.clock);
        MessageTo m = new MessageTo(o, this.clock, to);
        bus.postEvent(m);
    }

    @Subscribe
    public void onReceive(MessageTo m) {
        if (this.id == m.getIdDest()) { // the current process is the destination
            System.out.println(this.thread.getName() + " receives: " + m.getPayload()  + " for " + this.thread.getName());
            this.clock = Math.max(this.clock, m.getClock());
            this.clock++;
            System.out.println("New clock (" + this.thread.getName() + "): " + this.clock);
        }
    }


    public void run(){

        System.out.println(Thread.currentThread().getName() + " id: " + this.id);

        if(this.id == Process.nbProcess-1){
            Token t = new Token(this.id);
            bus.postEvent(t);
            System.out.println("Token thrown");
        }

        while(this.alive){
            try{
                Thread.sleep(500);

                if(Thread.currentThread().getName().equals("P1")){
                    // send

                    //broadcast("ga");

                    //sendTo("Hello", 1);

                    request();
                    //send message hello to 1
                    sendTo("Hello", 1);
                    release();


                }

            }catch(Exception e){

                e.printStackTrace();
            }

        }

        // liberation du bus
        this.bus.unRegisterSubscriber(this);
        this.bus = null;
        System.out.println(Thread.currentThread().getName() + " stopped");
        this.dead = true;
    }






    public void waitStopped(){
        while(!this.dead){
            try{
                Thread.sleep(500);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public void stop(){
        this.alive = false;
    }

    public int getClock() {
        return clock;
    }
}
