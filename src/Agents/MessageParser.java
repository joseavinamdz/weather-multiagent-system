package Agents;

import jade.core.Agent;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

import org.json.*;

/**
 * Created by JoseAlberto on 19/11/2015.
 */
public class MessageParser extends Agent {

    protected void setup()
    {
        MessageTemplate plantilla = AchieveREResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_QUERY);
        this.addBehaviour(new ParseMsg(this, plantilla));
    }

    private class ParseMsg extends AchieveREResponder {
        private String msg;
        private JSONObject req;

        public ParseMsg(Agent agente, MessageTemplate plantilla) {
            super(agente, plantilla);
        }

        protected ACLMessage handleRequest(ACLMessage request){
            msg = new String();
            req = new JSONObject(request.getContent());

            String [] parts = req.getString("message").split(",");

            for(String p : parts)
            {
                if(p.contains("@WSMultiagent"))
                {
                    p = p.replaceAll("@WSMultiagent", "");
                }

                msg += p + " ";
            }

            ACLMessage agree = request.createReply();
            agree.setPerformative(ACLMessage.AGREE);

            return agree;
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {
            ACLMessage inform = request.createReply();
            inform.setPerformative(ACLMessage.INFORM);
            inform.setContent("{\"userID\": " + req.getString("userID") + ", \"message\": \""
                    + msg + "\"}");

            return inform;
        }
    }
}
