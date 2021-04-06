import shutil
import tempfile

temp_dirs = []

PERMISSION_GROUP_MAPPING_DICT = {
"android.permission.READ_CALENDAR":"CALENDAR",
"android.permission.WRITE_CALENDAR":"CALENDAR",

"android.permission.CAMERA":"CAMERA",

"android.permission.GET_ACCOUNTS":"CONTACTS",
"android.permission.READ_CONTACTS":"CONTACTS",
"android.permission.WRITE_CONTACTS":"CONTACTS",

"android.permission.ACCESS_COARSE_LOCATION":"LOCATION",
"android.permission.ACCESS_FINE_LOCATION":"LOCATION",

"android.permission.RECORD_AUDIO":"MICROPHONE",

"android.permission.ACCESS_IMS_CALL_SERVICE":"PHONE",
"android.permission.ANSWER_PHONE_CALLS":"PHONE",
"android.permission.CALL_PHONE":"PHONE",
"android.permission.PROCESS_OUTGOING_CALLS":"PHONE",
"android.permission.READ_CALL_LOG":"PHONE",
"android.permission.READ_PHONE_NUMBERS":"PHONE",
"android.permission.READ_PHONE_STATE":"PHONE",
"android.permission.USE_SIP":"PHONE",
"android.permission.WRITE_CALL_LOG":"PHONE",
"com.android.voicemail.permission.ADD_VOICEMAIL":"PHONE",

"android.permission.READ_CELL_BROADCASTS":"SMS",
"android.permission.READ_SMS":"SMS",
"android.permission.RECEIVE_MMS":"SMS",
"android.permission.RECEIVE_SMS":"SMS",
"android.permission.RECEIVE_WAP_PUSH":"SMS",
"android.permission.SEND_SMS":"SMS",

"android.permission.READ_EXTERNAL_STORAGE":"STORAGE",
"android.permission.WRITE_EXTERNAL_STORAGE":"STORAGE",
"android.permission.EXTERNAL_PUBLIC_STORAGE":"STORAGE",
"android.permission.android.permission.EXTERNAL_PRIVATE_STORAGE":"STORAGE"
}

def make_temp_dir(prefix='', dir=''):
    global temp_dirs
    directory = tempfile.mkdtemp(prefix=prefix, dir=dir)
    temp_dirs.append(directory)
    return directory


def remove_temp_dir(directory):      
    shutil.rmtree(directory, ignore_errors=True)


def extract_number(cur_line):
    firstCom = cur_line.find("'")
    secondCom = cur_line.find("'", firstCom + 1)
    levelStr = cur_line[firstCom + 1: secondCom]
    return int(levelStr)

def extract_min_api(yml_path):
    return extract_sdk_api(yml_path, 'minSdkVersion')

def extract_max_api(yml_path):
    return extract_sdk_api(yml_path, 'maxSdkVersion')

def extract_target_api(yml_path):
    return extract_sdk_api(yml_path, 'targetSdkVersion')

def extract_sdk_api(yml_path, name):
    with open(yml_path, 'r') as fd:
        for line in fd.readlines():
            if name in line:
                return extract_number(line)
    return -1

def is_requested_dangerous_permissions(manifest_path):
    import xml.etree.ElementTree as ET
    root = ET.parse(manifest_path).getroot()
    per_set = set()
    for type_tag in root.findall('uses-permission'):
        per_set.add(type_tag.attrib['{http://schemas.android.com/apk/res/android}name'])
    
    for per in per_set:
        if per in PERMISSION_GROUP_MAPPING_DICT:
            return True
    return False