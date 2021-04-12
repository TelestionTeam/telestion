#!/usr/bin/env python3
# Code is only Python 3.8+ compatible

import os
import sys
import traceback
import xml.etree.ElementTree as ET
from typing import List, Tuple, Union


class Type:
    def __init__(self, name, size, java_repr):
        self._name = name
        self._size = size
        self._java_repr = java_repr

    def get_name(self):
        return self._name

    def get_size(self):
        return self._size

    def get_java_repr(self):
        return self._java_repr


class Types:
    UINT8_T = Type("uint8_t", 1, "UINT_8")
    UINT16_T = Type("uint16_t", 2, "UINT_16")
    UINT32_T = Type("uint32_t", 4, "UINT_32")
    UINT64_T = Type("uint64_t", 8, "UINT_64")
    INT8_T = Type("int8_t", 1, "INT_8")
    INT16_T = Type("int16_t", 2, "INT_16")
    INT32_T = Type("int32_t", 4, "INT_32")
    INT64_T = Type("int64_t", 8, "INT_64")
    FLOAT = Type("float", 4, "FLOAT")
    DOUBLE = Type("double", 8, "DOUBLE")
    CHAR = Type("char", 1, "CHAR")

    @classmethod
    def get_from_string(cls, name):
        if "uint8_t" in name:
            return cls.UINT8_T
        elif "uint16_t" in name:
            return cls.UINT16_T
        elif "uint32_t" in name:
            return cls.UINT32_T
        elif "uint64_t" in name:
            return cls.UINT64_T
        elif "int8_t" in name:
            return cls.INT8_T
        elif "int16_t" in name:
            return cls.INT16_T
        elif "int32_t" in name:
            return cls.INT32_T
        elif "int64_t" in name:
            return cls.INT64_T
        elif "float" in name:
            return cls.FLOAT
        elif "double" in name:
            return cls.DOUBLE
        elif "char" in name:
            return cls.CHAR
        else:
            print(name)


class Field:
    def __init__(self, mav_type: str, name: str, description: str, extension: bool):
        self._mav_type = Types.get_from_string(mav_type).get_name()
        if '[' in mav_type:
            self._mav_type += mav_type[mav_type.index('['):mav_type.index(']') + 1]
        self._name = name
        self._description = description
        self._extension = extension

    def get_name(self) -> str:
        return self._name

    def get_mav_type(self) -> str:
        return self._mav_type

    def get_description(self) -> str:
        return self._description

    def is_extension(self) -> bool:
        return self._extension


class Message:
    def __init__(self, msg_id: int, name: str, description: str, wip: bool, *fields: Field):
        self._msg_id = msg_id
        self._name = name
        self._description = description
        self._wip = wip
        self._fields = Message.sort_fields(fields)

    @staticmethod
    def _calc_crc(data: int, current_crc: int) -> int:
        data ^= (current_crc & 0xff)
        data ^= (data << 4)
        data &= 0xff
        return (current_crc >> 8) ^ (data << 8) ^ (data << 3) ^ (data >> 4) & 0xffff

    @staticmethod
    def sort_fields(fields: Union[Tuple[Field, ...], List[Field]]) -> List[Field]:
        return list(sorted(fields, key=lambda k: -Types.get_from_string(k.get_mav_type()).get_size()
                           if not k.is_extension() else 1))

    def calc_crc_extra(self) -> int:
        array = lambda x: str(chr(int(x[x.index('[') + 1:x.index(']')]))) if '[' in x else ''
        s = f"{self._name} " \
            + ''.join(x for x in
                      [f'{Types.get_from_string(f.get_mav_type()).get_name()} {f.get_name()} {array(f.get_mav_type())}'
                       for f in self._fields if not f.is_extension()])

        current_crc = 0xffff

        for c in s:
            current_crc = Message._calc_crc(ord(c), current_crc)

        return (current_crc & 0xFF) ^ (current_crc >> 8)

    def get_msg_id(self) -> int:
        return self._msg_id

    def get_name(self) -> str:
        return self._name

    def get_description(self) -> str:
        return self._description

    def get_fields(self) -> List[Field]:
        return self._fields

    def get_wip(self) -> bool:
        return self._wip

    def contains_array(self) -> bool:
        return any(['[' in f.get_mav_type() for f in self._fields])


def handle_args() -> Tuple[str, str, str]:
    args = sys.argv[1:]
    if len(args) == 0:
        return "", "", ""
    if args[0] in ['help', '-help', '-h']:
        print("Usage: 'python record.py -f <INPUT_FILE> -o <OUTPUT_PATH [Not required]>"
              " -p <OUTPUT_JAVA_PACKAGE [Not required]>'")
        print("If no output path is specified the specified java-package will be used. This is by default "
              "'org.telestion.adapter.mavlink.message'.")
        print("This help is available with 'python record.py -h'")
        return "", "", ""

    pos = {s: (i + 1) for i, s in enumerate(args) if s in ['-f', '-o', '-p']}

    return args[pos.get("-f")] if "-f" in pos else "", args[pos.get("-o")] if "-o" in pos else "", \
           args[pos.get("-p")] if "-p" in pos else ""


def get_messages(file: str) -> List[Message]:
    root = ET.parse(file).getroot()
    return [Message(msg.attrib['id'], msg.attrib['name'], msg.find('description').text, msg.find('wip') is not None,
                    *[Field(f.attrib['type'], f.attrib['name'], f.text, extension) for f in msg
                      if ((extension := (f.tag == 'extensions' or extension)) or True) and f.tag == 'field'])
            for msg in root.find('messages') if (extension := False) or True]  # If to reset extension


def to_record(msg: Message, output: str = "", package: str = "org.telestion.adapter.mavlink.message"):
    if package == "":
        package = "org.telestion.adapter.mavlink.message"
    if output == "":
        output = package
    name = msg.get_name().lower().replace("_", " ").title().replace(" ", "")
    new_line = '\n'
    template = f"""package {package};

import org.telestion.protocol.old_mavlink.annotation.MavField;
import org.telestion.protocol.old_mavlink.annotation.MavInfo;
import org.telestion.protocol.old_mavlink.annotation.NativeType;\
{f'{new_line}import org.telestion.protocol.old_mavlink.annotation.MavArray;' if msg.contains_array() else ''}\
{f'{new_line}import org.telestion.protocol.old_mavlink.message.MavlinkMessage;'
    if package != 'org.telestion.protocol.old_mavlink.message' else ''}

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {msg.get_description()}<br>
 * <br>
 * <i>Autogenerated by XML2Record-Tool v{VERSION}</i>
 * 
 * @author Autogenerated by XML2Record-Tool (by Cedric Boes)
 * @version 1.0 (autogenerated)\
{f'{new_line} * @deprecated This is still WIP and might change!' if msg.get_wip() else ''}
 */{f'{new_line}@Deprecated' if msg.get_wip() else ''}
@MavInfo(id = {msg.get_msg_id()}, crc = {msg.calc_crc_extra()})
@SuppressWarnings("preview")
public record {name}(/*TEMPLATE_RECORD_TYPES*/) implements MavlinkMessage {{
\t/**
\t * There shall be no default-constructor for normal developers.
\t */
\t@SuppressWarnings("unused")
\tprivate {name}() {{
\t\tthis(/*TEMPLATE_CONSTRUCTOR_ARGS*/);
\t}}
}}"""
    no_array = True
    for field in msg.get_fields():
        data_type = "int"
        array = None
        if "[" in field.get_mav_type():
            array = field.get_mav_type()[field.get_mav_type().index("["):]
            no_array = False
        if field.get_mav_type() in [Types.INT64_T.get_name(), Types.UINT32_T.get_name(), Types.UINT64_T.get_name()]:
            data_type = "long"
        elif field.get_mav_type() in [Types.FLOAT.get_name(), Types.DOUBLE.get_name()]:
            data_type = "double"
        elif field.get_mav_type() == Types.CHAR.get_name():
            data_type = "char"
        array_length = -1
        if array is not None:
            data_type += "[]"
            array_length = int(
                field.get_mav_type()[field.get_mav_type().index("[") + 1:field.get_mav_type().index("]")])
        template = template.replace("/*TEMPLATE_RECORD_TYPES*/", f"""
\t/**
\t * {field.get_description()}<br>
\t * <br>
\t * <i>Autogenerated by XML2Record-Tool v{VERSION}</i>
\t */{f'{os.linesep}        @MavArray(length = {array_length})' if array_length > 0 else ""}
\t@MavField(nativeType = NativeType.{Types.get_from_string(field.get_mav_type()).get_java_repr()}\
{', extension = true' if field.is_extension() else ''})
        @JsonProperty {data_type} {field.get_name()[:1].lower()}\
{field.get_name().lower().replace('_', ' ').title()[1:].replace(' ', '')}, /*TEMPLATE_RECORD_TYPES*/""") \
            .replace("/*TEMPLATE_CONSTRUCTOR_ARGS*/", f"\
{'null' if array_length > 0 else ('(char)' if Types.get_from_string(field.get_mav_type()) is Types.CHAR else '') + '0'}\
, /*TEMPLATE_CONSTRUCTOR_ARGS*/")
    template = template.replace(", /*TEMPLATE_RECORD_TYPES*/", "").replace(", /*TEMPLATE_CONSTRUCTOR_ARGS*/", "") \
        .replace("/*TEMPLATE_RECORD_TYPES*/", "").replace("/*TEMPLATE_CONSTRUCTOR_ARGS*/", "")
    if no_array:
        template = template.replace(f'{new_line}import org.telestion.adapter.mavlink.annotation.MavArray;', '')

    if '~' in output:
        output = output.replace('~', package)

    dot_start = False
    if output.startswith('..'):
        output = output[2:]
        dot_start = True

    if '.' in output:
        output = output.replace('.', '/')

    if dot_start:
        output = '..' + output

    if output[-1] != '/':
        output += '/'

    if not os.path.exists(output):
        os.makedirs(output)

    with open(file=output + f"{name}.java", mode='w') as f:
        f.write(template)


def interpret_file(file, output, package):
    print(f"Reading and interpreting MAVLink-File {file}...")
    messages = get_messages(file)
    print(f"Reading and interpreting MAVLink-File finished [{len(messages)} valid messages found]")

    print("Outputting to Java-Records (preview jdk-14)...")
    for count, message in enumerate(messages):
        print(f" Creating record for {message.get_name()} (id={message.get_msg_id()})... ".ljust(84), end='')
        total = len(messages)
        try:
            to_record(message, output, package)
            print("Success!".ljust(20), f"[{str(count + 1).rjust(len(str(total)))}/{total} "
                                        f"({str(int((count + 1) / len(messages) * 100)).rjust(3)}%)]", sep='')
        except Exception:
            print("Failed!".ljust(20), f"[{str(count).rjust(len(str(total)))}/{total} "
                                       f"({str(int((count + 1) / len(messages) * 100)).rjust(3)}%)]", sep='')
            traceback.print_exc()


def main():
    print("Starting MAVLink XML2Record-Tool")

    file, output, package = handle_args()
    if file != "":
        if '*' in file:
            print("Wildcard found!")
            path = file[:file.index('*')]
            file = [path + f for f in os.listdir(path) if f.endswith('.xml')]
        else:
            file = [file, ]

        for f in file:
            interpret_file(f, output.replace('*', f[:-4]), package.replace('*', f[f[:-4].rindex('/') + 1:-4]))
            print()

        print("All done!")
    else:
        print("No input file specified")

    print("Exiting MAVLink XML2Record-Tool")


VERSION = "1.3.11"

if __name__ == '__main__':
    main()
