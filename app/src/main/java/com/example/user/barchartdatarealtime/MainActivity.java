package com.example.user.barchartdatarealtime;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.example.user.barchartdatarealtime.model.AddressParameterRealTimeData;
import com.example.user.barchartdatarealtime.model.ParameterObjectRealTimeData;
import com.example.user.barchartdatarealtime.model.RealTimeStreamingData;
import com.example.user.barchartdatarealtime.utils.GsonUtil;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketOptions;

public class MainActivity extends AppCompatActivity {
    private LinearLayout linearLayout;
    private Button btnConnect,btnDisConnect;

    private int startId = 0;

    private Map<String, Integer> mIdMapping = new HashMap<String, Integer>();
    private WebSocketConnection mWebSocketConnection = new WebSocketConnection();
    private static final String TAG =
            MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init_view();

        init_onclick();
    }

    private void init_onclick() {
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToWebsocket("");
            }
        });

        btnDisConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disConnectToWebsocket();
            }
        });
    }

    private void disConnectToWebsocket() {
        if(mWebSocketConnection.isConnected()){
            mWebSocketConnection.disconnect();
        }
        mWebSocketConnection = null;
    }

    public static String getRandomColor() {
        String[] letters = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String color = "#";
        for (int i = 0; i < 6; i++ ) {
            color += letters[(int) Math.round(Math.random() * 15)];
        }
        return color;
    }

    BarChart barChart;
    WebSocket.WebSocketConnectionObserver realTimeRetrieveHandle;
    private void connectToWebsocket(String wsURL) {
        String webSocketRealTimeDataUri = "wss://dataengine.globiots.com:443/data-engine/mobile/realtime";
//        final String messageTogetDataRealTime = "{\"objects\": [{\"addresses\": [{\"address\":\"3000\",\"dataType\":\"integer_16\",\"length\":2,\"value\":\"\"},{\"address\":\"3002\",\"dataType\":\"integer_16\",\"length\":2,\"value\":\"\"},{\"address\":\"3004\",\"dataType\":\"integer_16\",\"length\":2,\"value\":\"\"},{\"address\": \"201A\",\"dataType\": \"float\",\"length\": 4},{\"address\": \"2020\",\"dataType\": \"float\",\"length\": 4},{\"address\": \"2000\",\"dataType\": \"float\",\"length\": 4}],\"hostname\": \"0.0.0.254\"}],\"sessionId\": \"\",\"timezone\": \"GMT+07:00\",\"updateTime\": 3}";

        final String messageTogetDataRealTime = "{\"objects\": [{\"addresses\": [{\"address\":\"301A\",\"dataType\":\"integer_16\",\"length\":2,\"value\":\"\"},{\"address\":\"2300\",\"dataType\":\"integer_16\",\"length\":2,\"value\":\"\"},{\"address\":\"2004\",\"dataType\":\"integer_16\",\"length\":2,\"value\":\"\"},{\"address\": \"2065\",\"dataType\": \"float\",\"length\": 4},{\"address\": \"2304\",\"dataType\": \"float\",\"length\": 4}],\"hostname\": \"0.0.1.4\"}],\"sessionId\": \"\",\"timezone\": \"GMT+07:00\",\"updateTime\": 3}";


        // start init data - build layout
        barChart = new BarChart(getApplicationContext());

        Description description = new Description();
        description.setText("");
        barChart.setDescription(description);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        barChart.setLayoutParams(params);

        linearLayout.addView(barChart);


        RealTimeStreamingData realTimeStreamingData = GsonUtil.getInstance()
                .fromJson (messageTogetDataRealTime, RealTimeStreamingData.class);

        List<IBarDataSet> iBarDataSetList = new ArrayList<>();
        for(ParameterObjectRealTimeData parameterObjectRealTimeData : realTimeStreamingData.getObjects()) {


            for (AddressParameterRealTimeData addressParameterRealTimeData : parameterObjectRealTimeData.getAddresses()) {

                String address = addressParameterRealTimeData.getAddress();

                BarDataSet barDataSet = new BarDataSet(new ArrayList<BarEntry>(), address);
                barDataSet.setColor(Color.parseColor(getRandomColor()));
                iBarDataSetList.add(barDataSet);

                startId++;
            }


        }
        BarData barData = new BarData(iBarDataSetList);

        barChart.setData(barData);
        try{
            realTimeRetrieveHandle =
                    new WebSocket.WebSocketConnectionObserver() {

                        @Override
                        public void onOpen() {
                            mWebSocketConnection.sendTextMessage(messageTogetDataRealTime);
                        }

                        @Override
                        public void onTextMessage(final String dataRealTime) {

                            RealTimeStreamingData realTimeStreamingData = GsonUtil.getInstance()
                                    .fromJson(dataRealTime, RealTimeStreamingData.class);
                            int columnIndex = 0;
                            for (ParameterObjectRealTimeData parameterObjectRealTimeData : realTimeStreamingData.getObjects()) {
                                for (AddressParameterRealTimeData addressParameterRealTimeData : parameterObjectRealTimeData.getAddresses()) {
                                    Float value = null;
                                    try {
                                        value = new Float(addressParameterRealTimeData.getValue());
                                    } catch (Exception e) {
                                        value = 0f;
                                        Log.w("Error", e.toString());
                                    }

                                    BarData barData = barChart.getBarData();
                                    IBarDataSet iBarDataSet = barData.getDataSetByIndex(columnIndex);
                                    BarDataSet barDataSet = (BarDataSet) iBarDataSet;
                                    if (barDataSet != null) {

                                        BarEntry entry = null;
                                        if (barDataSet.getValues().size() > 0) {
                                            entry = barDataSet.getEntryForIndex(0);
                                        }
                                        if (entry == null) {
                                            entry = new BarEntry(
                                                    columnIndex +1,
                                                    value,
                                                    true);
                                            barDataSet.addEntry(entry);
                                        } else {
                                            entry.setY(value);
                                        }

                                        barData.setValueFormatter(new DefaultValueFormatter(2));
                                        barData.notifyDataChanged();
                                        barChart.setBackgroundColor(Color.WHITE);
                                        barChart.getXAxis().setEnabled(false);
                                        barChart.getAxisRight().setEnabled(false);
                                        barChart.getAxisLeft().resetAxisMinimum();
                                        barChart.getAxisLeft().resetAxisMaximum();
                                        barChart.animateY(800);
                                        barChart.notifyDataSetChanged();
                                        barChart.invalidate();
                                        columnIndex++;
                                    }

                                }
                            }

                        }

                        @Override
                        public void onRawTextMessage(byte[] payload) {

                        }

                        @Override
                        public void onBinaryMessage(byte[] payload) {

                        }

                        @Override
                        public void onClose(final WebSocketCloseNotification code, final String reason) {

                        }
                    };
            WebSocketOptions websocketOptions = new WebSocketOptions();
            websocketOptions.setSocketConnectTimeout(15000);//ms ~ 15 s
            websocketOptions.setSocketReceiveTimeout(15000);//ms ~ 15 s

            mWebSocketConnection.connect(
                    new URI(webSocketRealTimeDataUri),
                    realTimeRetrieveHandle,
                    websocketOptions);

        } catch (final Exception e){
            Log.w("Error", e.toString());

        }
    }

    private void init_view() {
        linearLayout = findViewById(R.id.line1);
        btnConnect = findViewById(R.id.btnConnect);
        btnDisConnect = findViewById(R.id.btnDisConnect);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mWebSocketConnection.isConnected()){
            mWebSocketConnection.disconnect();
        }
        mWebSocketConnection = null;
    }
}
