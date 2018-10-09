package ir.mbf5923.searchbar.searchbarsdk;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.Toast;

import ir.mbf5923.searchbar.searchbar.mbfsearchbar;

public class MainActivity extends AppCompatActivity {

    mbfsearchbar search_bar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        search_bar = findViewById(R.id.search_bar);
        search_bar.setUrl("http://192.168.1.7/digimedad/mobserv/autosugestion");

    }


}
