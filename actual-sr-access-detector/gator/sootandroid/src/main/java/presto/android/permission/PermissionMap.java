/*
 * PermissionMap.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.permission;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import presto.android.Configs;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PermissionMap {
    private static PermissionMap instance;

    private final static Map<String, Set<String>> permissionMap = Maps.newHashMap();

    private final static Map<String, Set<String>> permissionGroupMap = Maps.newHashMap();

    public static synchronized PermissionMap v() {
        if (instance == null) {
            instance = new PermissionMap();
        }
        return instance;
    }

    private PermissionMap(){
        if (permissionMap.size() == 0) {
            parse();
        }
        init();
    }

    public boolean isPermissionCalled(String sig) {
        if (sig == null || sig.isEmpty()) return false;

        for (Set<String> permissionSigs : permissionMap.values()) {
            if(permissionSigs.contains(sig)) return true;
        }

        return false;
    }

    public String isContainedPermissionCalled(String string) {
        if (string == null || string.isEmpty()) return null;

        for (Set<String> permissionSigs : permissionMap.values()) {
            for (String per : permissionSigs) {
                if (string.contains(per)) {
                    return per;
                }
            }
        }

        return null;
    }

    public String getPermissionName(String sig) {
        for (String permissionNames : permissionMap.keySet()) {
            if(permissionMap.get(permissionNames).contains(sig)) {
                return permissionNames;
            }
        }

        return null;
    }

    public String getPermissionGroup(String permissions) {
        permissions = permissions.replaceAll("android.permission.","");

        Set<String> permissionGroups = Sets.newHashSet();
        String[] pers = permissions.split(",");
        for (String permission : pers) {
            for (String permissionGroup : permissionGroupMap.keySet()) {
                if (permissionGroupMap.get(permissionGroup).contains(permission)) {
                    permissionGroups.add(permissionGroup);
                    break;
                }
            }
        }

        return StringUtils.join(permissionGroups, ",");
    }

    private void init() {
        permissionGroupMap.put("CALENDAR", new HashSet<>(Arrays.asList("READ_CALENDAR", "WRITE_CALENDAR")));
        permissionGroupMap.put("CAMERA", new HashSet<>(Arrays.asList("CAMERA")));
        permissionGroupMap.put("CONTACTS", new HashSet<>(Arrays.asList("READ_CONTACTS", "WRITE_CONTACTS", "GET_ACCOUNTS")));
        permissionGroupMap.put("LOCATION", new HashSet<>(Arrays.asList("ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION")));
        permissionGroupMap.put("MICROPHONE", new HashSet<>(Arrays.asList("RECORD_AUDIO")));
        permissionGroupMap.put("PHONE", new HashSet<>(Arrays.asList("READ_CALL_LOG", "WRITE_CALL_LOG", "PROCESS_OUTGOING_CALLS","READ_PHONE_STATE", "READ_PHONE_NUMBERS", "CALL_PHONE", "ANSWER_PHONE_CALLS", "ADD_VOICEMAIL", "USE_SIP")));
        permissionGroupMap.put("SMS", new HashSet<>(Arrays.asList("SEND_SMS", "RECEIVE_SMS", "READ_SMS", "RECEIVE_WAP_PUSH", "RECEIVE_MMS")));
        permissionGroupMap.put("STORAGE", new HashSet<>(Arrays.asList("READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "EXTERNAL_PRIVATE_STORAGE", "EXTERNAL_PUBLIC_STORAGE")));
    }

    private void parse() {
        BufferedReader rdr = readFile(Configs.permissionMapFile);

        String line = null;
        String currentPermission = null;

        try {
            while ((line = rdr.readLine()) != null) {
                if (line.startsWith("Permission:"))
                    currentPermission = line.substring(11);
                else {
                    Set<String> permissions = permissionMap.get(currentPermission);
                    if (permissions == null) {
                        permissions = Sets.newHashSet();
                    }
                    permissions.add(line);
                    permissionMap.put(currentPermission, permissions);
                }
            }

            if (rdr != null)
                rdr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedReader readFile(String fileName) {
        BufferedReader br = null;
        try {
            Reader r = new FileReader(fileName);
            br = new BufferedReader(r);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        return br;
    }
}
