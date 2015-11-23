package wms.agents;

import jade.core.Agent;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

import org.json.*;

/**
 * Created by JoseAlberto on 19/11/2015.
 */

public class WeatherParser extends Agent {
    protected void setup() {
        /* Añadimos el comportamiento para la recepción de mensajes */
        MessageTemplate template = AchieveREResponder.createMessageTemplate(
                FIPANames.InteractionProtocol.FIPA_QUERY);
        this.addBehaviour(new Parser(this, template));
    }

    private class Parser extends AchieveREResponder {
        private String msg;
        private JSONObject req;

        public Parser(Agent a, MessageTemplate template) {
            super(a, template);
        }

        protected ACLMessage handleRequest(ACLMessage request) throws RefuseException {
            msg = new String();
            req = new JSONObject(request.getContent());
            WebDriver driver = new HtmlUnitDriver();

            /* Buscamos el tiempo en Internet */
            driver.get("http://www.google.es/");

            WebElement search = driver.findElement(By.name("q"));
            search.sendKeys(req.getString("message") + " weather");
            search.submit();

            /* Parseamos los datos recibidos y nos quedamos con lo que buscamos */
            Document doc = Jsoup.parse(driver.getPageSource());
            Elements temp = doc.select("span[class=wob_t]");
            Elements desc = doc.select("img[class=_Lbd]");
            Elements hum = doc.select("td:contains(Humedad:)");

            driver.close();

            /* Si hemos conseguido lo que buscabamos */
            if(temp.size() > 0) {
                /* Creamos el mensaje de aceptación para su envío */
                ACLMessage agree = request.createReply();
                agree.setPerformative(ACLMessage.AGREE);

                /* Preparamos el mensaje que se enviará en el informe */
                msg = "\n" + desc.get(0).attr("alt") + "\n"
                        + "Temperatura: " + temp.get(0).text() + "\n"
                        + "Viento: " + temp.get(1).text() + "\n"
                        + hum.get(1).text();

                return agree;
            } else {
                /* Si no hemos encontrado el tiempo buscado, se lanza una excepción */
                throw new RefuseException("No se ha encontrado el tiempo buscado.");
            }
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {
            ACLMessage inform = request.createReply();
            inform.setPerformative(ACLMessage.INFORM);

            /* Si estamos respondiendo un mensaje sin usuario, enviamos solo el tiempo */
            if(req.getString("userID").equals("null"))
            {
                inform.setContent("[" + req.getString("message") + "]" + "\n" + msg);
            } else {
                /* En caso contrario, enviamos el usuario junto con el tiempo solicitado */
                inform.setContent("@" + req.getString("userID") + "\n" + msg);
            }

            return inform;
        }
    }
}
