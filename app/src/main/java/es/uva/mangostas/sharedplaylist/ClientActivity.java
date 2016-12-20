package es.uva.mangostas.sharedplaylist;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import es.uva.mangostas.sharedplaylist.BluetoothService.BTSharedPlayService;
import es.uva.mangostas.sharedplaylist.BluetoothService.Constants;
import es.uva.mangostas.sharedplaylist.BluetoothService.DeviceListActivity;
import es.uva.mangostas.sharedplaylist.Model.ShpMediaObject;

public class ClientActivity extends AppCompatActivity {


    //Codigos de los Intent
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final String TYPE = "Client";
    private static final int VIDEO_SELECTED = 1;
    private static final int SONG_SELECTED = 3;

    private ListView listView;
    private ArrayList<ShpMediaObject> playList;
    private ArrayAdapter<ShpMediaObject> adapter;
    private BluetoothAdapter btAdapter;
    private BTSharedPlayService mService;
    private BluetoothDevice device;

    //Interfaz
    public SearchBox search;
    private FloatingActionsMenu fab;
    private com.getbase.floatingactionbutton.FloatingActionButton fab_yt;
    private com.getbase.floatingactionbutton.FloatingActionButton fab_local;


    //Manejador para devolver información al servicio
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BTSharedPlayService.STATE_CONNECTED:
                            break;
                        case BTSharedPlayService.STATE_CONNECTING:
                            break;
                        case BTSharedPlayService.STATE_LISTEN:
                        case BTSharedPlayService.STATE_NONE:
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    Toast.makeText(getApplicationContext(), R.string.songsended, Toast.LENGTH_LONG).show();
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    Toast.makeText(getApplicationContext(), R.string.conected, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.client);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        search = (SearchBox) findViewById(R.id.searchbox);
        fab = (FloatingActionsMenu) findViewById(R.id.menu_fab);
        fab_yt = (com.getbase.floatingactionbutton.FloatingActionButton) findViewById(R.id.action_yt);
        fab_local = (com.getbase.floatingactionbutton.FloatingActionButton) findViewById(R.id.action_fs);
        fab_yt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSearchbox();
            }
        });

        fab_local.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                startActivityForResult(intent, SONG_SELECTED);
            }

        });

        //Colocamos los escuchadores de la barra de busqueda de youtube
        setSearchBoxListeners();

        // Get ListView object from xml
        listView = (ListView) findViewById(R.id.listView);

        // Defined Array playList to show in ListView
        playList = new ArrayList<>();


        // Define a new Adapter
        // First parameter - Context
        // Second parameter - Layout for the row
        // Third parameter - ID of the TextView to which the data is written
        // Forth - the Array of data

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, playList);


        // Assign adapter to ListView
        listView.setAdapter(adapter);

        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item index
                int itemPosition = position;

                // ListView Clicked item value
                String itemValue = (String) listView.getItemAtPosition(position);

                // Show Alert
                Toast.makeText(getApplicationContext(),
                        "Position :" + itemPosition + "  ListItem : " + itemValue, Toast.LENGTH_LONG)
                        .show();

            }

        });

        //Ponemos el servicio en funcionamiento
        setupService();

    }

    /**
     * Ponemos en funcionamiento el servicio para conectarnos.
     */
    private void setupService() {
        //Inicializamos el servicio de Envio.
        mService = new BTSharedPlayService(getApplicationContext(), mHandler, TYPE);
        mService.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Hacer el dispositivo visible
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    /**
     * Metodo para hacer el dispositivo visible para los demas
     */
    private void ensureDiscoverable() {
        if(btAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Metodo que envia el codigo de un video al dispositivo conectado
     * @param msg Codigo del video
     * @param name Nombre del video de YouTube
     */
    private void sendVideo(String msg, String name, String channel) {
        //Comprobamos que estamos conectados antes de enviar
        Log.d("ESTADO CLIENTE", ""+mService.getState());
        if (mService.getState() != BTSharedPlayService.STATE_CONNECTED_AND_LISTEN) {
            Toast.makeText(getApplicationContext(), R.string.notavaliablesendwithoutconection, Toast.LENGTH_LONG).show();
            return;
        }

        //Comprobamos que hay algo en el mensaje para enviar
        if (msg.length() > 0) {
            //Creamos un array de bytes para enviar el cual tiene 4 bytes mas para el tamaño
            // que en el caso de los videos es 0.
            byte[] message = msg.getBytes();
            byte[] videoName = name.getBytes();
            byte[] videoChannel = channel.getBytes();
            byte[] video = new byte[msg.length()+64];
            video[0] = (byte) (0 >> 24);
            video[1] = (byte) (0 >> 16);
            video[2] = (byte) (0 >> 8);
            video[3] = (byte) (0 /*>> 0*/);

            //Añadimos el nombre de la cancion en los 30 bytes despues del tamaño
            for (int i = 0; i < videoName.length; i++) {
                video[i+4] = videoName[i];
            }
            //Añadimos el canal del video en los 30 bytes siguientes al nombre
            for (int i = 0; i < videoChannel.length; i++) {
                video[i+34] = videoChannel[i];
            }
            //Copiamos los datos del ID del video al array y lo enviamos
            for (int i = 0; i < message.length; i++) {
                video[i+64] = message[i];
            }
            mService.write(video);
        }
    }

    /**
     * Metodo que envía la canción seleccionada al dispositivo conectado
     * @param path Ruta de la canción a enviar.
     */
    public void sendSong(String path) {
        //Definimos el archivo que se va  enviar a traves de su path de almacenamiento
        File song = new File(path);

        //Declaramos un array de bytes el cual tiene el tamaño del archivo mas cuatro bytes para el tamaño del mismo
        byte[] songArray = new byte[(int) song.length() + 4];

        //Guardamos y casteamos el tamaño del archivo al array de bytes
        int tam = (int) song.length();
        songArray[0] = (byte) (tam >> 24);
        songArray[1] = (byte) (tam >> 16);
        songArray[2] = (byte) (tam >> 8);
        songArray[3] = (byte) (tam);

        try {
            //Escribimos sobre el array de bytes los datos del fichero a traves de un inputStream
            FileInputStream fis = new FileInputStream(song);
            fis.read(songArray, 4, (int) song.length());
            fis.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }

        //Finalmente si tenemos conexion enviamos el archivo a traves del servicio
        if (mService.getState() != BTSharedPlayService.STATE_CONNECTED_AND_LISTEN) {
            Toast.makeText(getApplicationContext(), R.string.notavaliablesendwithoutconection, Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(getApplicationContext(), R.string.sending, Toast.LENGTH_LONG).show();
            mService.write(songArray);
        }
    }

    /**
     * Conecta un dispositivo con otro a traves de los datos recibidos por el intent de conexión
     * @param data Intent con la información para la conexión.
     */
    private void connectDevice(Intent data) {
        //Obtenemos la MAC
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        //Obtenemos el objeto de dispositivo
        device = btAdapter.getRemoteDevice(address);
        //Intentamos conectar
        mService.connect(device);
        Log.w("ESTADO CLIENTE CON", ""+mService.getState());
    }

    /**
     * Metodos para mostrar la barra de busqueda
     */
    private void showSearchbox () {
        search.revealFromMenuItem(R.id.action_yt, this);
    }

    /**
     * Metodo para asociar los listeners para la barra
     * de busqueda
     */
    private void setSearchBoxListeners() {

        search.setSearchListener(new SearchBox.SearchListener() {

            @Override
            public void onSearchOpened() {
                fab.collapse();
            }

            @Override
            public void onSearchClosed() {
                closeSearch();
            }

            @Override
            public void onSearchTermChanged(final String term) {
            }

            @Override
            public void onSearch(final String searchTerm) {
                SearchResult result = new SearchResult(searchTerm);
                search.addSearchable(result);
                startYoutubeResultsActivity(searchTerm);
            }

            @Override
            public void onResultClick(SearchResult result) {
                //React to result being clicked
                Log.d("ytSearch", "Result");
            }

            @Override
            public void onSearchCleared() {

            }

        });
    }

    /**
     * Metodo que lanza el intent para realizar la busqueda
     * de YT.
     * @param searchTerm Termino de busqueda para la petición
     */
    private void startYoutubeResultsActivity(final String searchTerm) {
        Intent intent = new Intent(this, YoutubeResultsActivity.class);
        intent.putExtra("term" ,searchTerm);

        startActivityForResult(intent, VIDEO_SELECTED);
    }

    /**
     * Cierra la barra de busqueda
     */
    protected void closeSearch() {
        search.hideCircularly(this);
    }

    /**
     * Metodo que traduce una URI a un path del alamacenamiento
     * interno del telefono
     * @param Uri URI a traducit
     * @param helper Datos de ayuda para la traducción
     * @param context Contexto de la aplicacion
     * @return Path del URI introducido
     */
    public String getRealPathFromURI(Context context, Uri Uri, String helper){

        if (Uri.toString().length() >= 64) {
            String predireccion = "";
            String direccion;
            String falsaUri = Uri.toString();
            boolean esInterna = false;
            boolean esExterna = false;
            String falsaUriRecortada = falsaUri.substring(57);
            falsaUriRecortada = falsaUriRecortada.substring(0, 7);
            if (falsaUriRecortada.equals("primary")) {
                esInterna = true;
            } else esExterna = true;
            int pos = buscarDosPuntos(helper);
            direccion = helper.substring(pos);
            if (pos == 0) System.exit(-1);
            if (esInterna == true) {
                predireccion = "/storage/emulated/0/";
                predireccion = predireccion + direccion;
            } else if (esExterna == true) {
                predireccion = "/storage/sdcard/";
                predireccion = predireccion + direccion;
            } else {
                System.exit(-1);
            }
            return predireccion;
        } else {
            Cursor cursor = null;
            try {
                String[] proj = { MediaStore.Audio.Media.DATA };
                cursor = context.getContentResolver().query(Uri,  proj, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

        }
    }

    /**
     * Metodo complementario del metodo getRealPath, busca el carácter ":" dentro de cadena y devuelve su posición
     * @param cadena Cadena en la que se buscaran el caracter ":"
     * @return Posición del caracter ":"
     */
    private static int buscarDosPuntos(String cadena){
        int i=0;
        int k=1;
        int j=cadena.length();
        boolean parar=false;
        String aux;
        try{
            do{
                aux=cadena.substring(i,k);
                if(aux.equals(":")){
                    parar=true;
                }else{
                    i++;
                    k++;
                }
            }while(parar==false && i<=j);

            return k;
        }catch(Exception e){
            return 0;
        }
    }

    /**
     * Metodo que gestiona los resultados devueltos por los intent que lanza la actividad.
     *
     * @param requestCode Código de la petición
     * @param resultCode  Código del resultado de la petición
     * @param data        Datos devueltos por la petición.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            //Caso de la petición de conexion.
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // Cuando vuelve con el dispositivo al que desea conectarse
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;
            //Caso de la seleccion de video
            case VIDEO_SELECTED:
                // Cuando la peticion de seleccionar un video de la lista vuelve
                if (resultCode == Activity.RESULT_OK) {
                    String video = data.getStringExtra("videoID");
                    String name = data.getStringExtra("videoName");
                    String channel = data.getStringExtra("videoChannel");
                    Log.d("VIDEOID", video);
                    sendVideo(video, name, channel);
                } else {

                }
                break;
            //Caso de la seleccion de cancion
            case SONG_SELECTED:
                //Cuando recibimos la seleccion de una cancion
                if (resultCode == Activity.RESULT_OK) {
                    String path=getRealPathFromURI(getApplicationContext(), data.getData(), data.getData().getPath());
                    Log.d("PATH", path);
                    sendSong(path);

                } else {
                    // Ha ocurrido un error con la canción seleccionada
                    Toast.makeText(getApplicationContext(), R.string.songnotavaliable, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

}


