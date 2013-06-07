package eu.lucidcode.myBusSchedule;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import eu.lucidcode.myTimeTable.R;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class TableFragment extends Fragment {

    private final String from;
    private final String to;

    private View thisView;

    public TableFragment(String from, String to) {
        this.from = from;
        this.to = to;
    }

    private final SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm");

    private final SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        new CurrentConnectionsService(from, to).execute();
        thisView = inflater.inflate(R.layout.table, container, false);
        fetchCurrentConnections();
        return thisView;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchCurrentConnections();
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (menuVisible) {
            fetchCurrentConnections();
        }
    }

    private void fetchCurrentConnections() {
        new CurrentConnectionsService(from, to).execute();
    }

    private class CurrentConnectionsService extends AsyncTask<Void, Void, List<Connection>> {

        private final String from;
        private final String to;

        private CurrentConnectionsService(String from, String to) {
            this.from = from;
            this.to = to;
        }

        private final static String QUANDO_SERVICE_URL = "http://webservice.qando.at/2.0/webservice.ft";

        protected String getXMLCommand(Calendar time) {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
                    "<ft> \n" +
                    "    <request clientId=\"123\" apiName=\"api_get_route\" apiVersion=\"2.0\"> \n" +
                    "        <client clientId=\"123\"/> \n" +
                    "        <requestType>api_get_route</requestType> \n" +
                    "        <outputCoords>WGS84</outputCoords> \n" +
                    "        <from>" + from + "</from> \n" +
                    "        <fromType>stop</fromType>\n" +
                    "        <to>" + to + "</to> \n" +
                    "        <toType>stop</toType> \n" +
                    "        <year>" + time.get(Calendar.YEAR) + "</year> \n" +
                    "        <month>" + (time.get(Calendar.MONTH) + 1) + "</month> \n" +
                    "        <day>" + time.get(Calendar.DAY_OF_MONTH) + "</day> \n" +
                    "        <hour>" + time.get(Calendar.HOUR_OF_DAY) + "</hour> \n" +
                    "        <minute>" + time.get(Calendar.MINUTE) + "</minute> \n" +
                    "        <deparr>dep</deparr> \n" +
                    "        <modality>pt</modality> \n" +
                    "        <sourceFrom>stoplist</sourceFrom> \n" +
                    "        <sourceTo>stoplist</sourceTo> \n" +
                    "    </request> \n" +
                    "</ft>";
        }

        @Override
        protected List<Connection> doInBackground(Void... params) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            try {
                HttpPost post = new HttpPost(QUANDO_SERVICE_URL);
                post.setEntity(new StringEntity(getXMLCommand(new GregorianCalendar()), "UTF-8"));
                HttpResponse response = httpClient.execute(post, localContext);
                return parseConnections(response.getEntity().getContent());
            } catch (Throwable t) {
                t.printStackTrace();
                return new ArrayList<Connection>();
            }
        }

        @Override
        protected void onPostExecute(List<Connection> connections) {
            TableLayout tableLayout = (TableLayout) thisView;
            tableLayout.setStretchAllColumns(true);
            tableLayout.removeAllViews();
            for (Connection c : connections) {
                TableRow row = new TableRow(getActivity().getApplicationContext());

                TextView vehicle = new TextView(getActivity().getApplicationContext());
                vehicle.setText(c.vehicle);
                row.addView(vehicle, 0);

                TextView in = new TextView(getActivity().getApplicationContext());
                in.setText(c.getDepartureMinutes().toString());
                row.addView(in, 1);

                TextView start = new TextView(getActivity().getApplicationContext());
                start.setText(displayFormat.format(c.start));
                row.addView(start, 2);

                tableLayout.addView(row);
            }
        }

        protected List<Connection> parseConnections(InputStream content) throws XPathExpressionException, ParseException {
            XPath xpath = XPathFactory.newInstance().newXPath();
            InputSource inputSource = new InputSource(content);
            NodeList trips = (NodeList) xpath.evaluate("//trips/trip", inputSource, XPathConstants.NODESET);
            List<Connection> connections = new ArrayList<Connection>(trips.getLength());
            for (int i = 0; i < trips.getLength(); i++) {
                Node trip = trips.item(i);
                Date start = parseFormat.parse(((Attr) xpath.evaluate("./timePlanned/time/@start", trip, XPathConstants.NODE)).getValue());
                Date end = parseFormat.parse(((Attr) xpath.evaluate("./timePlanned/time/@end", trip, XPathConstants.NODE)).getValue());
                String vehicle = ((Attr) xpath.evaluate("./segments/segment/vehicle/@name", trip, XPathConstants.NODE)).getValue();
                connections.add(new Connection(vehicle, start, end));
            }

            // drop the first result as it seems to be the first connection for the whole day
            connections.remove(0);

            return connections;

        }
    }


}
