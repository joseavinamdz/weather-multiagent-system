package wms.agents;

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

    protected void setup() {
        /* Añadimos el comportamiento para la recepción de mensajes */
        MessageTemplate plantilla = AchieveREResponder.createMessageTemplate(
                FIPANames.InteractionProtocol.FIPA_QUERY);
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

            /* Parseamos el mensaje recibido para conseguir los datos necesarios */
            String [] parts = req.getString("message").split(",");

            for(String p : parts)
            {
                if(p.contains("@WSMultiagent"))
                {
                    p = p.replaceAll("@WSMultiagent", "");
                }

                msg += p + " ";
            }

            /* Creamos el mensaje de aceptación y lo devolvemos */
            ACLMessage agree = request.createReply();
            agree.setPerformative(ACLMessage.AGREE);

            return agree;
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {
            /* Respondemos con el mensaje preparado anteriormente */
            ACLMessage inform = request.createReply();
            inform.setPerformative(ACLMessage.INFORM);
            inform.setContent("{\"userID\": " + req.getString("userID") + ", \"message\": \""
                    + msg + "\"}");

            return inform;
        }
    }
}
