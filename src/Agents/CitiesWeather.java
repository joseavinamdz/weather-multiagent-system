package wms.agents;

import jade.core.Agent;

import java.io.File;
import java.sql.*;

import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import org.json.JSONObject;


/**
 * Created by JoseAlberto on 23/11/2015.
 */

public class CitiesWeather extends Agent {
    private Connection db = null;

    public void setup() {
        /* Comprobamos si la base de datos "city.db" existe */
        File f = new File("city.db");
        if(!f.exists()) {
            try {
                /* Si no existe la creamos y le añadimos los datos */
                db = DriverManager.getConnection("jdbc:sqlite:city.db");
                System.out.println("Base de Datos creada correctamente");

                Statement st = db.createStatement();
                st.execute("PRAGMA encoding = \"UTF-16\"");
                String sql = "CREATE TABLE CITIES " +
                        "(ID INT PRIMARY KEY     NOT NULL," +
                        " NAME           TEXT    NOT NULL," +
                        " COUNTRY        TEXT    NOT NULL)";
                st.executeUpdate(sql);

                sql = "INSERT INTO CITIES (ID,NAME,COUNTRY) " +
                        "VALUES (1, 'Ciudad Real', 'España');";
                st.executeUpdate(sql);

                sql = "INSERT INTO CITIES (ID,NAME,COUNTRY) " +
                        "VALUES (2, 'Madrid', 'España');";
                st.executeUpdate(sql);

                sql = "INSERT INTO CITIES (ID,NAME,COUNTRY) " +
                        "VALUES (3, 'Barcelona', 'España');";
                st.executeUpdate(sql);

                sql = "INSERT INTO CITIES (ID,NAME,COUNTRY) " +
                        "VALUES (4, 'Valencia', 'España');";
                st.executeUpdate(sql);

                sql = "INSERT INTO CITIES (ID,NAME,COUNTRY) " +
                        "VALUES (5, 'Sevilla', 'España');";
                st.executeUpdate(sql);

                sql = "INSERT INTO CITIES (ID,NAME,COUNTRY) " +
                        "VALUES (6, 'Zaragoza', 'España');";
                st.executeUpdate(sql);

                sql = "INSERT INTO CITIES (ID,NAME,COUNTRY) " +
                        "VALUES (7, 'Malaga', 'España');";
                st.executeUpdate(sql);

                sql = "INSERT INTO CITIES (ID,NAME,COUNTRY) " +
                        "VALUES (8, 'Murcia', 'España');";
                st.executeUpdate(sql);

                sql = "INSERT INTO CITIES (ID,NAME,COUNTRY) " +
                        "VALUES (9, 'Palma de Mallorca', 'España');";
                st.executeUpdate(sql);

                sql = "INSERT INTO CITIES (ID,NAME,COUNTRY) " +
                        "VALUES (10, 'Las Palmas de Gran Canaria', 'España');";
                st.executeUpdate(sql);

                sql = "INSERT INTO CITIES (ID,NAME,COUNTRY) " +
                        "VALUES (11, 'Bilbao', 'España');";
                st.executeUpdate(sql);

                st.close();

                /* Añadimos el comportamiento para la recepción de mensajes */
                MessageTemplate template = AchieveREResponder.createMessageTemplate(
                        FIPANames.InteractionProtocol.FIPA_QUERY);
                this.addBehaviour(new Search(this, template));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            try {
                /* Si existe, solo accedemos a ella */
                db = DriverManager.getConnection("jdbc:sqlite:city.db");
                System.out.println("Base de Datos abierta correctamente");

                /* Añadimos el comportamiento para la recepción de mensajes */
                MessageTemplate template = AchieveREResponder.createMessageTemplate(
                        FIPANames.InteractionProtocol.FIPA_QUERY);
                this.addBehaviour(new Search(this, template));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void takeDown() {
        try {
            /* Cerramos la base de datos al terminar el agente */
            db.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private class Search extends AchieveREResponder {
        private String msg;
        private JSONObject req;

        public Search(Agent a, MessageTemplate template) {
            super(a, template);
        }

        protected ACLMessage handleRequest(ACLMessage request) throws RefuseException {
            try{
                /* Accedemos a una fila aleatoria de la tabla CITIES de la base de datos */
                Statement st = db.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM CITIES ORDER BY RANDOM() LIMIT 1;");

                /* Cogemos los datos de esa fila y creamos la cadena "Ciudad Pais" */
                msg = new String();
                while(rs.next())
                {
                    msg += rs.getString("name");
                    msg += ", " + rs.getString("country");
                }

                /* Creamos el mensaje de aceptación y lo devolvemos */
                ACLMessage agree = request.createReply();
                agree.setPerformative(ACLMessage.AGREE);

                return agree;
            } catch (SQLException e) {
                /* Si hubo algún error con la base de datos, se aborta y se manda una excepción */
                throw new RefuseException("Hubo un error con la base de datos.");
            }
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {
            /* Respondemos con el mensaje preparado anteriormente */
            ACLMessage inform = request.createReply();
            inform.setPerformative(ACLMessage.INFORM);
            inform.setContent("{\"userID\": \"null\", \"message\": \"" + msg + "\"}");

            return inform;
        }
    }
}
