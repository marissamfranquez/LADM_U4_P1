package mx.edu.ittepic.unidad4p1

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.CallLog
import android.telecom.Call
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.sql.SQLException

class MainActivity : AppCompatActivity() {
    var permisoConcedido = 24
    var listaContactos = ArrayList<ArrayList<String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var permisoRegLlamadas = android.Manifest.permission.READ_CALL_LOG
        var permisoEnviarSms = android.Manifest.permission.SEND_SMS
        var permissionGranted = PackageManager.PERMISSION_GRANTED

        if( ActivityCompat.checkSelfPermission(this, permisoRegLlamadas) != permissionGranted ||
            ActivityCompat.checkSelfPermission(this, permisoEnviarSms) != permissionGranted ) {
            ActivityCompat.requestPermissions(this, arrayOf(permisoRegLlamadas, permisoEnviarSms), permisoConcedido)
        }
        getListaContacto()
        button.setOnClickListener {
            if(validateField()){
                addContacto()
            }
        }
        button2.setOnClickListener {
            if(validateMessages()){
                enviarMensajes()
            }
        }
    }
    fun validateMessages(): Boolean{
        return when {
            editText3.text.toString().isEmpty() -> {
                Toast.makeText(this, "Agrega un mensaje en caso de que el contacto te caiga bien", Toast.LENGTH_LONG)
                    .show()
                editText3.requestFocus()
                false
            }
            editText4.text.toString().isEmpty() -> {
                Toast.makeText(this, "Agrega un mensaje en caso de que el contacto te caiga mal", Toast.LENGTH_LONG)
                    .show()
                editText4.requestFocus()
                false
            }
            else -> true
        }
    }
    fun validateField(): Boolean {
        return when {
            editText.text.toString().isEmpty() -> {
                Toast.makeText(this, "Falta el nombre", Toast.LENGTH_LONG)
                    .show()
                editText.requestFocus()
                false
            }
            editText2.text.toString().length != 10 -> {
                Toast.makeText(this, "Falta el número de 10 dígitos", Toast.LENGTH_LONG)
                    .show()
                editText2.requestFocus()
                false
            }
            else -> true
        }
    }

    fun addContacto() {
        try {
            var baseDatos = BaseDatos(this, "LISTA", null, 1)
            var insert = baseDatos.writableDatabase
            var deseado = if(switch1.isChecked)
                            "1"
                          else
                            "0"
            var sql = "INSERT INTO LISTA VALUES('${editText.text}','${editText2.text}', '$deseado')"
            insert.execSQL(sql)
            baseDatos.close()
            editText.setText("")
            editText2.setText("")
            switch1.isChecked = false
            getListaContacto()
        }catch (e: SQLException) {
            Toast.makeText(this, "Error SQLException", Toast.LENGTH_LONG).show()
        }
    }

    fun getListaContacto() {
        textView.text = "Lista de contactos\n"
        listaContactos = ArrayList()
        try {
            var cursor = BaseDatos(this, "LISTA", null, 1).readableDatabase
                         .rawQuery("SELECT * FROM LISTA", null)
            if(cursor.moveToFirst()){
                do{
                    var nombreContacto = cursor.getString(0)
                    var telefonoContacto = cursor.getString(1)
                    var deseado = cursor.getString(2)
                    var esDeseado = if(deseado == "1")
                                        "Si me cae bien"
                                    else
                                        "No me cae bien"
                    listaContactos.add(arrayListOf(nombreContacto, telefonoContacto, deseado))
                    textView.text = "${textView.text}" + "\n$nombreContacto, $telefonoContacto\n$esDeseado\n"
                }while(cursor.moveToNext())
            }
        }catch (e: SQLiteException) {
            Toast.makeText(this, "Error SQLiteException", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    fun enviarMensajes() {
        var cr = baseContext.contentResolver
        var c = cr.query(CallLog.Calls.CONTENT_URI, null, null, null, null)
        if(c != null){
            var numeros = ArrayList<String>()
            if(c.moveToFirst()){
                do{
                    var columnaTipo = c.getColumnIndex(CallLog.Calls.TYPE)
                    var tipoLlamadaPerdida = CallLog.Calls.MISSED_TYPE
                    if(c.getString(columnaTipo).toInt() == tipoLlamadaPerdida) {
                        var numero = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                        for(contacto in listaContactos){
                            if(contacto[1] == numero){
                                if(!numeros.contains(numero)) {
                                    if (contacto[2] == "1") {
                                        SmsManager.getDefault().sendTextMessage(numero, null, editText3.text.toString(),
                                                                                null, null)
                                    } else {
                                        SmsManager.getDefault().sendTextMessage(numero, null, editText4.text.toString(),
                                            null, null)
                                    }
                                    numeros.add(numero)
                                    break
                                }
                            }
                        }
                    }
                }while(c.moveToNext())

            }
            c.close()
        }

    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            permisoConcedido -> {
                Toast.makeText(this, "Los permisos necesarios fueron concedidos", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
