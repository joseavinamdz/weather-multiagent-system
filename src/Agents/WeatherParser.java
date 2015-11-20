package Agents;

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
    protected void setup()
    {
        MessageTemplate plantilla = AchieveREResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_QUERY);
        this.addBehaviour(new Parser(this, plantilla));
    }

    private class Parser extends AchieveREResponder {
        private String msg;
        private JSONObject req;

        public Parser(Agent agente, MessageTemplate plantilla) {
            super(agente, plantilla);
        }

        protected ACLMessage handleRequest(ACLMessage request) throws RefuseException {
            msg = new String();
            req = new JSONObject(request.getContent());
            WebDriver driver = new HtmlUnitDriver();

            driver.get("http://www.google.es/");

            WebElement search = driver.findElement(By.name("q"));
            search.sendKeys(req.getString("message") + " weather");
            search.submit();

            Document doc = Jsoup.parse(driver.getPageSource());
            Elements temp = doc.select("span[class=wob_t]");
            Elements desc = doc.select("img[class=_Lbd]");
            Elements hum = doc.select("td:contains(Humedad:)");

            driver.close();

            if(temp.size() > 0) {
                ACLMessage agree = request.createReply();
                agree.setPerformative(ACLMessage.AGREE);

                msg = "\n" + desc.get(0).attr("alt") + "\n"
                        + "Temperatura: " + temp.get(0).text() + "\n"
                        + "Viento: " + temp.get(1).text() + "\n"
                        + hum.get(1).text();

                return agree;
            } else {
                throw new RefuseException("No se ha encontrado el tiempo buscado.");
            }
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {
            ACLMessage inform = request.createReply();
            inform.setPerformative(ACLMessage.INFORM);
            inform.setContent("@" + req.getString("userID") + "\n" + msg);

            return inform;
        }
    }
}
