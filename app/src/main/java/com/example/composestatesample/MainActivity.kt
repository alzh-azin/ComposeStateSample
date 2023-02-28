package com.example.composestatesample

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.composestatesample.ui.theme.ComposeStateSampleTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.parcelize.Parcelize

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeStateSampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    HelloScreen()
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Composable
fun HelloScreen() {
    //region remember
    var personalData by remember {
        mutableStateOf(PersonalData(""))
    }

    //endregion remember

    //region mapSaver
    val city = run {

        val nameKey = "Name"
        val countryKey = "Country"

        mapSaver(
            save = { mapOf(nameKey to it.name, countryKey to it.country) },
            restore = { City(it[nameKey] as String, it[countryKey] as String) }
        )
    }

    var cityMapSaver by rememberSaveable(
        stateSaver = city
    ) {
        mutableStateOf(City("", ""))
    }

    //endregion mapSaver

    //region listSaver

    val cityList = listSaver(
        save = { listOf(it.name, it.country) },
        restore = { City(it[0], it[1]) }
    )

    var cityListSaver by rememberSaveable(
        stateSaver = cityList
    ) {
        mutableStateOf(City("", ""))
    }

    //endregion listSaver

    Column {
        HelloContent("Parcelize Example", personalData) {
            personalData = PersonalData(it)
        }

        CityContent("MapSaver Example", cityMapSaver) {
            cityMapSaver = it
        }

        CityContent("MapList Example", cityListSaver) {
            cityListSaver = it
        }
    }


}

@Composable
fun HelloContent(title: String, personalData: PersonalData, onNameChange: (String) -> Unit) {

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.body1
        )
        OutlinedTextField(
            value = personalData.name,
            onValueChange = onNameChange,
            label = { Text("Name") }
        )
    }
}

@Composable
fun CityContent(title: String, city: City, onCityChange: (City) -> Unit) {

    Row {
        Column(modifier = Modifier.padding(16.dp).weight(1f)) {
            Text(
                text = title,
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.body1
            )
            OutlinedTextField(
                value = city.country,
                onValueChange = { country ->
                    onCityChange.invoke(city.copy(country = country))
                },
                label = { Text("Country") }
            )
        }
        Column(modifier = Modifier.padding(16.dp).weight(1f)) {
            Text(
                text = title,
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.body1
            )
            OutlinedTextField(
                value = city.name,
                onValueChange = { cityName ->
                    onCityChange.invoke(city.copy(name = cityName))
                },
                label = { Text("City") }
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposeStateSampleTheme {
        Greeting("Android")
    }
}

@Parcelize
data class PersonalData(var name: String) : Parcelable

data class City(val name: String, val country: String)