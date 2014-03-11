// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.core.extension.api.box2d;

import android.content.Intent;
import android.util.JsonToken;
import android.util.Log;
import android.util.JsonReader;
import android.util.JsonWriter;

import org.chromium.base.JNINamespace;
import org.chromium.base.CalledByNative;
import org.xwalk.core.extension.XWalkExtension;
import org.xwalk.core.extension.XWalkExtensionContext;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@JNINamespace("xwalk::box2d")
public class Box2DExtension extends XWalkExtension {
    public final static String JS_API_PATH = "jsapi/box2d_api.js";
    public final static String NAME = "xwalk.box2d";

    final private static String TAG = "Box2DExtension";
    final private static String KEY_CMD = "command";
    final private static String KEY_DATA = "data";
    final private static String VALUE_DATA_SUCCESS = "success";
    final private static String KEY_ERROR = "error";
    final private static String KEY_PARAM_PREFIX = "param";
    final private static int MAX_PARAMS_COUNT = 10;

    private ArrayList<Object> internalArray = new ArrayList<Object>();
    private Point internalPoint = new Point(0, 0);

    private class Point {
        double x;
        double y;
        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
        public void clear() {
            x = 0;
            y = 0;
        }
    }

    private class Contact {
        int fixtureAId;
        int fixtureBId;
        boolean isTouching;
        Contact(int a, int b, boolean touching) {
            this.fixtureAId = a;
            this.fixtureBId = b;
            this.isTouching = touching;
        }
    }

    public Box2DExtension(String jsApi, XWalkExtensionContext context) {
        super(NAME, jsApi, context);
    }

    @Override
    public void onMessage(int instanceId, String message) {
        postMessage(instanceId, handleMessage(instanceId, message));
    }

    @Override
    public String onSyncMessage(int instanceId, String message) {
        return handleMessage(instanceId, message);
    }

    private Object parseJSON(JsonReader reader) throws IOException, IllegalStateException {
        JsonToken token = reader.peek();
        switch (token) {
        case BEGIN_ARRAY:
            ArrayList<Object> array = new ArrayList<Object>();
            reader.beginArray();
            while (reader.hasNext()) {
                array.add(parseJSON(reader));
            }
            reader.endArray();
            return array;
        case BEGIN_OBJECT:
            Map<String, Object> map = new HashMap<String, Object>();
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                map.put(name, parseJSON(reader));
            }
            reader.endObject();
            return map;
        case BOOLEAN:
            return reader.nextBoolean();
        case NUMBER:
            try {
                return reader.nextInt();
            } catch (NumberFormatException e) {
                return reader.nextDouble();
            }
        case STRING:
            return reader.nextString();
        case END_ARRAY:
        case END_DOCUMENT:
        case END_OBJECT:
        case NAME:
        case NULL:
        default:
            reader.skipValue();
            return null;
        }
    }

    private String handleMessage(int instanceId, String message) {
        StringReader contents = new StringReader(message);
        JsonReader reader = new JsonReader(contents);

        String cmd = null;
        Object[] params = new Object[MAX_PARAMS_COUNT];
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals(KEY_CMD)) {
                    cmd = reader.nextString();
                } else if (name.startsWith(KEY_PARAM_PREFIX)) {
                    int param_idx = -1;
                    try {
                        param_idx = Integer.parseInt(name.substring(KEY_PARAM_PREFIX.length()));
                    } catch (NumberFormatException e) {
                    }
                    if (param_idx > 0 && param_idx <= MAX_PARAMS_COUNT) {
                        params[param_idx-1] = parseJSON(reader);
                    } else {
                        reader.skipValue();
                    }
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Error: " + e);
            Log.e(TAG, message);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error: " + e);
            Log.e(TAG, message);
        }
        return handleCommand(cmd, params);
    }

    private String handleCommand(String cmd, Object[] params) {
        String error = "";
        if (cmd == null) {
            error = "command not specified";
            return handleError(error);
        } else if (cmd.equals("createWorld")) {
            return createWorld(cmd, params);
        } else if (cmd.equals("deleteBody")) {
            return deleteBody(cmd, params);
        } else if (cmd.equals("createDistanceJoint")) {
            return createDistanceJoint(cmd, params);
        } else if (cmd.equals("setContinuous")) {
            return setContinuous(cmd, params);
        } else if (cmd.equals("setGravity")) {
            return setGravity(cmd, params);
        } else if (cmd.equals("step")) {
            return step(cmd, params);
        } else if (cmd.equals("getLastContacts")) {
            return getLastContacts(cmd, params);
        } else if (cmd.equals("clearForces")) {
            return clearForces(cmd, params);
        } else if (cmd.equals("setSensor")) {
            return setSensor(cmd, params);
        } else if (cmd.equals("setDensity")) {
            return setDensity(cmd, params);
        } else if (cmd.equals("setFriction")) {
            return setFriction(cmd, params);
        } else if (cmd.equals("setRestitution")) {
            return setRestitution(cmd, params);
        } else if (cmd.equals("createBody")) {
            return createBody(cmd, params);
        } else if (cmd.equals("createFixture")) {
            return createFixture(cmd, params);
        } else if (cmd.equals("setBodyTransform")) {
            return setBodyTransform(cmd, params);
        } else if (cmd.equals("getLinearVelocity")) {
            return getLinearVelocity(cmd, params);
        } else if (cmd.equals("getWorldCenter")) {
            return getWorldCenter(cmd, params);
        } else if (cmd.equals("getLocalCenter")) {
            return getLocalCenter(cmd, params);
        } else if (cmd.equals("applyImpulse")) {
            return applyImpulse(cmd, params);
        } else if (cmd.equals("isAwake")) {
            return isAwake(cmd, params);
        } else if (cmd.equals("getAngularVelocity")) {
            return getAngularVelocity(cmd, params);
        } else if (cmd.equals("setAwake")) {
            return setAwake(cmd, params);
        } else if (cmd.equals("setLinearVelocity")) {
            return setLinearVelocity(cmd, params);
        } else if (cmd.equals("applyForceToCenter")) {
            return applyForceToCenter(cmd, params);
        } else if (cmd.equals("setLinearDamping")) {
            return setLinearDamping(cmd, params);
        } else if (cmd.equals("setAngularVelocity")) {
            return setAngularVelocity(cmd, params);
        } else if (cmd.equals("setActive")) {
            return setActive(cmd, params);
        } else if (cmd.equals("getObjectContacts")) {
            return getObjectContacts(cmd, params);
        } else {
            error = "invalid command: " + cmd;
            return handleError(error);
        }
    }

    private boolean checkParams(Object[] params, int count) {
        for (int i = 0; i < count; i++) {
            if (params[i] == null) return false;
        }
        return true;
    }

    private String handleError(String error) {
        return "{ '" + KEY_ERROR + "': '" + error + "'}";
    }

    private double getDoubleValue(Object num) {
        if (num == null) {
            return 0;
        } else if (num instanceof Integer) {
            return (Integer) num;
        } else if (num instanceof Double) {
            return (Double) num;
        } else {
            return 0;
        }
    }

    /*
     * Native call wrapper
     */
    private String createWorld(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 3)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        double x,y;
        boolean doSleep;
        StringWriter contents = new StringWriter();
        try {
            x = getDoubleValue(params[0]);
            y = getDoubleValue(params[1]);
            doSleep = (Boolean)(params[2]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            writer.name(KEY_DATA).value(nativeCreateWorld(x, y, doSleep));
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();        
    }

    private String deleteBody(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 2)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world,body;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeDeleteBody(world, body);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();        
    }

    private String createDistanceJoint(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 3)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, bodyA, bodyB;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            bodyA = (Integer)(params[1]);
            bodyB = (Integer)(params[2]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeCreateDistanceJoint(world, bodyA, bodyB);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();        
    }

    private String setContinuous(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 2)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world;
        boolean continuous;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            continuous = (Boolean)(params[1]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeSetContinuous(world, continuous);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String setGravity(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 3)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world;
        double x, y;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            x = getDoubleValue(params[1]);
            y = getDoubleValue(params[2]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeSetGravity(world, x, y);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String step(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 4)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, vi, pi;
        double dt;
        String contents = "";
        try {
            world = (Integer)(params[0]);
            dt = getDoubleValue(params[1]);
            vi = (Integer)(params[2]);
            pi = (Integer)(params[3]);
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        }
        contents = nativeStep(world, dt, vi, pi);
        return contents.toString();
    }

    private String getLastContacts(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 1)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeGetLastContacts(world);
            ArrayList<Object> contacts = internalArray;
            writer.name(KEY_DATA).beginArray();
            writer.value(contacts.size());
            for (Object obj : contacts) {
                if (obj instanceof Contact) {
                    Contact contact = (Contact) obj;
                    writer.value(contact.fixtureAId);
                    writer.value(contact.fixtureBId);
                    writer.value(contact.isTouching);
                } else {
                    writer.value(-1);
                    writer.value(-1);
                    writer.value(false);
                }
            }
            contacts.clear();
            writer.endArray();
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String clearForces(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 1)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeClearForces(world);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String setSensor(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 3)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, fixture;
        boolean isSenser;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            fixture = (Integer)(params[1]);
            isSenser = (Boolean)(params[2]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeSetSensor(world, fixture, isSenser);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String setDensity(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 3)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, fixture;
        float density;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            fixture = (Integer)(params[1]);
            density = (float) getDoubleValue(params[2]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeSetDensity(world, fixture, density);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String setFriction(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 3)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, fixture;
        float friction;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            fixture = (Integer)(params[1]);
            friction = (float) getDoubleValue(params[2]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeSetFriction(world, fixture, friction);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String setRestitution(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 3)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, fixture;
        float restitution;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            fixture = (Integer)(params[1]);
            restitution = (float) getDoubleValue(params[2]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeSetRestitution(world, fixture, restitution);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    @SuppressWarnings("unchecked")
    private String createBody(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 2)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, type = 0;
        double x = 0, y = 0;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            Map<String, Object> bodyDef = (HashMap<String, Object>) params[1];
            Map<String, Object> position = (HashMap<String, Object>) bodyDef.get("position");
            x = getDoubleValue(position.get("x"));
            y = getDoubleValue(position.get("y"));
            type = (Integer)(bodyDef.get("type"));
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            writer.name(KEY_DATA).value(nativeCreateBody(world, type, x, y));
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        } catch (ClassCastException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (NullPointerException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);            
        }

        return contents.toString();
    }

    @SuppressWarnings("unchecked")
    private String createFixture(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 3)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        String type = null;
        double width = 0, height = 0, radius = 0;
        double friction = 0, restitution = 0, density = 0 , p1, p2;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            Map<String, Object> fixtureDef = (HashMap<String, Object>) params[2];
            friction = getDoubleValue(fixtureDef.get("friction"));
            restitution = getDoubleValue(fixtureDef.get("restitution"));
            density = getDoubleValue(fixtureDef.get("density"));
            Map<String, Object> shape = (HashMap<String, Object>) fixtureDef.get("shape");
            type = shape.get("type").toString();
            radius = getDoubleValue(shape.get("radius"));
            width = getDoubleValue(shape.get("width"));
            height = getDoubleValue(shape.get("height"));
            if (type == null) {
                p1 = 0;
                p2 = 0;
            } else if (type.equals("circle")) {
                p1 = radius;
                p2 = 0;
            } else if (type.equals("box")) {
                p1 = width;
                p2 = height;
            } else {
                p1 = 0;
                p2 = 0;
            }
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            writer.name(KEY_DATA).value(nativeCreateFixture(
                    world, body, friction, restitution, density, type, p1, p2));
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        } catch (ClassCastException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (NullPointerException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);            
        }

        return contents.toString();
    }

    private String setBodyTransform(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 5)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        double x, y ,angle;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            x = getDoubleValue(params[2]);
            y = getDoubleValue(params[3]);
            angle = getDoubleValue(params[4]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeSetBodyTransform(world, body, x, y, angle);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String getLinearVelocity(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 2)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeGetLinearVelocity(world, body);
            Point p = internalPoint;
            writer.name(KEY_DATA).beginArray();
            writer.value(p.x);
            writer.value(p.y);
            internalPoint.clear();
            writer.endArray();
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String getWorldCenter(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 2)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeGetWorldCenter(world, body);
            Point p = internalPoint;
            writer.name(KEY_DATA).beginArray();
            writer.value(p.x);
            writer.value(p.y);
            internalPoint.clear();
            writer.endArray();
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String getLocalCenter(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 2)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeGetLocalCenter(world, body);
            Point p = internalPoint;
            writer.name(KEY_DATA).beginArray();
            writer.value(p.x);
            writer.value(p.y);
            internalPoint.clear();
            writer.endArray();
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String applyImpulse(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 7)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        double ix, iy, px, py;
        boolean wake;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            ix = getDoubleValue(params[2]);
            iy = getDoubleValue(params[3]);
            px = getDoubleValue(params[4]);
            py = getDoubleValue(params[5]);
            wake = (Boolean)(params[6]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeApplyImpulse(world, body, ix, iy, px, py, wake);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String isAwake(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 2)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            writer.name(KEY_DATA).value(nativeIsAwake(world, body));
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String getAngularVelocity(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 2)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            writer.name(KEY_DATA).value(nativeGetAngularVelocity(world, body));
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String setAwake(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 3)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        boolean wake;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            wake = (Boolean)(params[2]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeSetAwake(world, body, wake);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String setLinearVelocity(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 4)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        double x, y;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            x = getDoubleValue(params[2]);
            y = getDoubleValue(params[3]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeSetLinearVelocity(world, body, x, y);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String applyForceToCenter(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 5)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        double x, y;
        boolean wake;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            x = getDoubleValue(params[2]);
            y = getDoubleValue(params[3]);
            wake = (Boolean)(params[4]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeApplyForceToCenter(world, body, x, y, wake);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String setLinearDamping(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 3)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        double damp;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            damp = getDoubleValue(params[2]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeSetLinearDamping(world, body, damp);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String setAngularVelocity(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 3)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        double w;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            w = getDoubleValue(params[2]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeSetAngularVelocity(world, body, w);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String setActive(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 3)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        boolean active;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            active = (Boolean)(params[2]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeSetActive(world, body, active);
            writer.name(KEY_DATA).value(VALUE_DATA_SUCCESS);
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }

    private String getObjectContacts(String cmd, Object[] params) {
        String error = "";
        if (!checkParams(params, 2)) {
            error = "invalid params count for command: " + cmd;
            return handleError(error);                
        }
        int world, body;
        StringWriter contents = new StringWriter();
        try {
            world = (Integer)(params[0]);
            body = (Integer)(params[1]);
            JsonWriter writer = new JsonWriter(contents);
            writer.beginObject();
            nativeGetObjectContacts(world, body);
            ArrayList<Object> contacts = internalArray;
            writer.name(KEY_DATA).beginArray();
            for (Object obj : contacts) {
                if (obj instanceof Integer) {
                    Integer contact = (Integer) obj;
                    writer.value(contact.intValue());
                } else {
                    writer.value(-1);
                }
            }
            contacts.clear();
            writer.endArray();
            writer.endObject();
            writer.close();
        } catch (NumberFormatException e) {
            error = "invalid params for command: " + cmd;
            return handleError(error);
        } catch (IOException e) {
            error = "failed to write result as json";
            return handleError(error);
        }

        return contents.toString();
    }
    
    @CalledByNative
    private void createArray() {
        internalArray.clear();
    }

    @CalledByNative
    private void returnPoint(double x, double y) {
        internalPoint.x = x;
        internalPoint.y = y;
    }

    @CalledByNative
    private void createContact(int a, int b, boolean touching) {
        internalArray.add(new Contact(a, b, touching));
    }

    @CalledByNative
    private void createInteger(int n) {
        internalArray.add(n);
    }
    //--------------------------------------------------------------------------------------------
    //  Native methods
    //--------------------------------------------------------------------------------------------
    private native int nativeCreateWorld(double x, double y, boolean doSleep);
    private native void nativeDeleteBody(int worldId, int bodyId);
    private native void nativeCreateDistanceJoint(int worldId, int bodyAId, int bodyBId);
    private native void nativeSetContinuous(int worldId, boolean continuous);
    private native void nativeSetGravity(int worldId, double x, double y);
    private native String nativeStep(
            int worldId, double dt, int velocityIterations, int positionIterations);
    private native void nativeGetLastContacts(int worldId);
    private native void nativeClearForces(int worldId);
    private native void nativeSetSensor(int worldId, int fixtureId, boolean isSensor);
    private native void nativeSetDensity(int worldId, int fixtureId, float density);
    private native void nativeSetFriction(int worldId, int fixtureId, float friction);
    private native void nativeSetRestitution(int worldId, int fixtureId, float restitution);
    private native int nativeCreateBody(int worldId, int type, double x, double y);
    private native int nativeCreateFixture(int worldId, int bodyId,
            double friction, double restitution, double density,
            String type, double param1, double param2);
    private native void nativeSetBodyTransform(int worldId, int bodyId, double x, double y, double angle);
    private native void nativeGetLinearVelocity(int worldId, int bodyId);
    private native void nativeGetWorldCenter(int worldId, int bodyId);
    private native void nativeGetLocalCenter(int worldId, int bodyId);
    private native void nativeApplyImpulse(int worldId, int bodyId,
            double impluse_x, double impluse_y,
            double point_x, double point_y, boolean wake);
    private native boolean nativeIsAwake(int worldId, int bodyId);
    private native float nativeGetAngularVelocity(int worldId, int bodyId);
    private native void nativeSetAwake(int worldId, int bodyId, boolean wake);
    private native void nativeSetLinearVelocity(int worldId, int bodyId, double x, double y);
    private native void nativeApplyForceToCenter(int worldId, int bodyId, double x, double y, boolean wake);
    private native void nativeSetLinearDamping(int worldId, int bodyId, double damp);
    private native void nativeSetAngularVelocity(int worldId, int bodyId, double w);
    private native void nativeSetActive(int worldId, int bodyId, boolean active);
    private native void nativeGetObjectContacts(int worldId, int bodyId);
}
