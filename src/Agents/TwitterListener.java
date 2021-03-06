package wms.agents;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;

import jade.core.behaviours.TickerBehaviour;
import twitter4j.*;

import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.domain.FIPANames;
import jade.core.behaviours.ThreadedBehaviourFactory;

/**
 * Created by JoseAlberto on 18/11/2015.
 */

public class TwitterListener extends Agent {
    /* Keys para el acceso a la aplicacion creada en TwitterDev */
    private final static String CONSUMER_KEY = "XXXXXXX";
    private final static String CONSUMER_KEY_SECRET = "XXXXXXX";
    private final static String ACCESS_TOKEN = "XXXXXXX";
    private final static String ACCESS_TOKEN_SECRET = "XXXXXXX";

    Twitter twitter;
    TwitterStream twitterStream;
    ThreadedBehaviourFactory tbf;

    protected void setup() {
        this.tbf = new ThreadedBehaviourFactory();
         /* Creamos nuestra propia configuración */
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(CONSUMER_KEY);
        builder.setOAuthConsumerSecret(CONSUMER_KEY_SECRET);
        builder.setOAuthAccessToken(ACCESS_TOKEN);
        builder.setOAuthAccessTokenSecret(ACCESS_TOKEN_SECRET);

        Configuration conf = builder.build();

        /* Creamos nuestra instancia de Twitter */
        this.twitter = new TwitterFactory(conf).getInstance();
        this.twitterStream = new TwitterStreamFactory(conf).getInstance();

        /* Dejamos la ejecucion del listener en un hilo dedicado */
        this.addBehaviour(tbf.wrap(new Listener(this)));
        /* Dejamos la ejecución de la consulta cíclica (cada 5 min) en otro hilo dedicado */
        this.addBehaviour(tbf.wrap(new WakeCity(this, 300000)));
    }

    protected void takeDown()
    {
        /* Paramos todos los hilos manejados por este ThreadedBehaviourFactory */
        tbf.interrupt();
    }

    private class Listener extends OneShotBehaviour
    {
        Agent myAgent;

        public Listener(Agent a)
        {
            super(a);
            this.myAgent = a;
        }

        public void onStart()
        {
            /* Creamos nuestro UserStreamListener personal */
            UserStreamListener listener = new UserListener(this.myAgent);
            /* Agregamos nuestro listener al Stream de Twitter */
            twitterStream.addListener(listener);
        }

        public void action()
        {
            /* Escuchamos a la espera de recibir mensajes a responder */
            twitterStream.user();
        }
    }

    private class UserListener implements UserStreamListener {
        Agent myAgent;

        public UserListener(Agent a)
        {
            this.myAgent = a;
        }

        @Override
        public void onDeletionNotice(long l, long l1) {}

        @Override
        public void onFriendList(long[] longs) {}

        @Override
        public void onFavorite(User user, User user1, Status status) {}

        @Override
        public void onUnfavorite(User user, User user1, Status status) {}

        @Override
        public void onFollow(User user, User user1) {}

        @Override
        public void onUnfollow(User user, User user1) {}

        @Override
        public void onDirectMessage(DirectMessage directMessage) {}

        @Override
        public void onUserListMemberAddition(User user, User user1, UserList userList) {}

        @Override
        public void onUserListMemberDeletion(User user, User user1, UserList userList) {}

        @Override
        public void onUserListSubscription(User user, User user1, UserList userList) {}

        @Override
        public void onUserListUnsubscription(User user, User user1, UserList userList) {}

        @Override
        public void onUserListCreation(User user, UserList userList) {}

        @Override
        public void onUserListUpdate(User user, UserList userList) {}

        @Override
        public void onUserListDeletion(User user, UserList userList) {}

        @Override
        public void onUserProfileUpdate(User user) {}

        @Override
        public void onUserSuspension(long l) {}

        @Override
        public void onUserDeletion(long l) {}

        @Override
        public void onBlock(User user, User user1) {}

        @Override
        public void onUnblock(User user, User user1) {}

        @Override
        public void onRetweetedRetweet(User user, User user1, Status status) {}

        @Override
        public void onFavoritedRetweet(User user, User user1, Status status) {}

        @Override
        public void onQuotedTweet(User user, User user1, Status status) {}

        public void onStatus(Status status) {
            if (!status.isRetweet() && status.getText().contains("@WSMultiagent")) {
                /* Mandamos mensaje a MessageParser */
                ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);

                /* Mensaje con estructura JSON */
                msg.setContent("{\"userID\": " + status.getUser().getScreenName() + ", \"message\": \""
                        + status.getText() + "\"}");

                AID id = new AID();
                id.setLocalName("MessageParser");
                msg.addReceiver(id);
                msg.setSender(this.myAgent.getAID());

                this.myAgent.addBehaviour(new MsgInitiator(this.myAgent, msg));
            }
        }
        @Override
        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}

        @Override
        public void onTrackLimitationNotice(int numberOfLimitedStatuses) {}

        @Override
        public void onScrubGeo(long l, long l1) {}

        @Override
        public void onStallWarning(StallWarning stallWarning) {}

        @Override
        public void onException(Exception ex) {
            ex.printStackTrace();
        }
    }

    private class WakeCity extends TickerBehaviour {
        public WakeCity(Agent a, long p) {
            super(a, p);
        }

        public void onTick() {
            /* Mandamos mensaje a CitiesWeather */
            ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);

            AID id = new AID();
            id.setLocalName("CitiesWeather");
            msg.addReceiver(id);
            msg.setSender(this.myAgent.getAID());

            this.myAgent.addBehaviour(new MsgInitiator(this.myAgent, msg));
        }
    }

    private class MsgInitiator extends AchieveREInitiator
    {
        public MsgInitiator(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        protected void handleRefuse(ACLMessage refuse) {
            System.out.println(refuse.getContent());
        }

        protected void handleInform(ACLMessage inform)
        {
            String sender = inform.getSender().getName().split("@")[0];
            /* Si el resultado de la comparacion es 0, entonces es la cadena que buscamos */
            if(sender.compareToIgnoreCase("WeatherParser") == 0) {
                try {
                    twitter.updateStatus(inform.getContent());
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            } else {
                /* Enviamos mensaje a WeatherParser */
                ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);

                /* Mensaje con estructura JSON */
                msg.setContent(inform.getContent());

                AID id = new AID();
                id.setLocalName("WeatherParser");
                msg.addReceiver(id);
                msg.setSender(myAgent.getAID());

                myAgent.addBehaviour(new MsgInitiator(myAgent, msg));
            }
        }
    }
}
