package ht.milleniumgroup.pos;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.util.Base64;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * MILLENNIUM POS — impression sur imprimante thermique Bluetooth classique (SPP).
 * Méthodes JS : listPaired(), connect({address}), write({data: base64}), disconnect(), status().
 */
@CapacitorPlugin(
    name = "BluetoothPrinter",
    permissions = { @Permission(alias = "bluetooth", strings = { Manifest.permission.BLUETOOTH_CONNECT }) }
)
public class BluetoothPrinterPlugin extends Plugin {

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothSocket socket;
    private OutputStream out;
    private String connectedName = "";

    private boolean needsPermission() {
        return Build.VERSION.SDK_INT >= 31 && getPermissionState("bluetooth") != PermissionState.GRANTED;
    }

    private BluetoothAdapter adapter() {
        BluetoothManager manager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        return manager == null ? null : manager.getAdapter();
    }

    /* ── Imprimantes appairées ─────────────────────────────── */

    @PluginMethod
    public void listPaired(PluginCall call) {
        if (needsPermission()) {
            requestPermissionForAlias("bluetooth", call, "afterPermissionList");
            return;
        }
        doList(call);
    }

    @PermissionCallback
    private void afterPermissionList(PluginCall call) {
        if (needsPermission()) {
            call.reject("Autorisation Bluetooth refusée.");
            return;
        }
        doList(call);
    }

    private void doList(PluginCall call) {
        BluetoothAdapter a = adapter();
        if (a == null) {
            call.reject("Bluetooth non disponible sur cet appareil.");
            return;
        }
        if (!a.isEnabled()) {
            call.reject("Activez le Bluetooth de l'appareil.");
            return;
        }
        JSArray devices = new JSArray();
        try {
            Set<BluetoothDevice> bonded = a.getBondedDevices();
            if (bonded != null) {
                for (BluetoothDevice d : bonded) {
                    JSObject o = new JSObject();
                    o.put("name", d.getName() == null ? d.getAddress() : d.getName());
                    o.put("address", d.getAddress());
                    devices.put(o);
                }
            }
        } catch (SecurityException e) {
            call.reject("Autorisation Bluetooth refusée.");
            return;
        }
        JSObject result = new JSObject();
        result.put("devices", devices);
        call.resolve(result);
    }

    /* ── Connexion ─────────────────────────────────────────── */

    @PluginMethod
    public void connect(PluginCall call) {
        if (needsPermission()) {
            requestPermissionForAlias("bluetooth", call, "afterPermissionConnect");
            return;
        }
        doConnect(call);
    }

    @PermissionCallback
    private void afterPermissionConnect(PluginCall call) {
        if (needsPermission()) {
            call.reject("Autorisation Bluetooth refusée.");
            return;
        }
        doConnect(call);
    }

    private void doConnect(final PluginCall call) {
        final String address = call.getString("address");
        if (address == null || address.isEmpty()) {
            call.reject("Adresse de l'imprimante manquante.");
            return;
        }
        new Thread(() -> {
            try {
                closeQuietly();
                BluetoothAdapter a = adapter();
                if (a == null) throw new Exception("Bluetooth non disponible.");
                BluetoothDevice device = a.getRemoteDevice(address);
                try {
                    a.cancelDiscovery();
                } catch (SecurityException ignored) {
                    // sans importance
                }
                BluetoothSocket s = device.createRfcommSocketToServiceRecord(SPP_UUID);
                s.connect();
                socket = s;
                out = s.getOutputStream();
                connectedName = device.getName() == null ? address : device.getName();
                JSObject result = new JSObject();
                result.put("connected", true);
                result.put("name", connectedName);
                call.resolve(result);
            } catch (Exception e) {
                closeQuietly();
                call.reject("Connexion impossible : " + e.getMessage());
            }
        }).start();
    }

    /* ── Impression ────────────────────────────────────────── */

    @PluginMethod
    public void write(PluginCall call) {
        String data = call.getString("data");
        if (out == null) {
            call.reject("Imprimante non connectée.");
            return;
        }
        if (data == null) {
            call.reject("Aucune donnée à imprimer.");
            return;
        }
        try {
            out.write(Base64.decode(data, Base64.DEFAULT));
            out.flush();
            call.resolve();
        } catch (Exception e) {
            closeQuietly();
            call.reject("Échec de l'impression : " + e.getMessage());
        }
    }

    @PluginMethod
    public void disconnect(PluginCall call) {
        closeQuietly();
        call.resolve();
    }

    @PluginMethod
    public void status(PluginCall call) {
        JSObject result = new JSObject();
        result.put("connected", socket != null && socket.isConnected());
        result.put("name", connectedName);
        call.resolve(result);
    }

    private void closeQuietly() {
        try {
            if (out != null) out.close();
        } catch (Exception ignored) {
            // déjà fermé
        }
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {
            // déjà fermé
        }
        out = null;
        socket = null;
    }
}
