# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: ComponentSpecificationMessage.proto

from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import descriptor_pb2
# @@protoc_insertion_point(imports)


import InterfaceSpecificationMessage_pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='ComponentSpecificationMessage.proto',
  package='android.vts',
  serialized_pb='\n#ComponentSpecificationMessage.proto\x12\x0b\x61ndroid.vts\x1a#InterfaceSpecificationMessage.proto\"\x9d\x03\n\x1d\x43omponentSpecificationMessage\x12\x34\n\x0f\x63omponent_class\x18\x01 \x01(\x0e\x32\x1b.android.vts.ComponentClass\x12\x32\n\x0e\x63omponent_type\x18\x02 \x01(\x0e\x32\x1a.android.vts.ComponentType\x12!\n\x16\x63omponent_type_version\x18\x03 \x01(\x02:\x01\x31\x12\x16\n\x0e\x63omponent_name\x18\x04 \x01(\x0c\x12\x0f\n\x07package\x18\x0b \x01(\x0c\x12\x0e\n\x06import\x18\x0c \x03(\x0c\x12%\n\x1coriginal_data_structure_name\x18\xe9\x07 \x01(\x0c\x12\x0f\n\x06header\x18\xea\x07 \x03(\x0c\x12>\n\tinterface\x18\xd1\x0f \x01(\x0b\x32*.android.vts.InterfaceSpecificationMessage\x12>\n\nattributes\x18\xb5\x10 \x03(\x0b\x32).android.vts.VariableSpecificationMessage')




_COMPONENTSPECIFICATIONMESSAGE = _descriptor.Descriptor(
  name='ComponentSpecificationMessage',
  full_name='android.vts.ComponentSpecificationMessage',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='component_class', full_name='android.vts.ComponentSpecificationMessage.component_class', index=0,
      number=1, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='component_type', full_name='android.vts.ComponentSpecificationMessage.component_type', index=1,
      number=2, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='component_type_version', full_name='android.vts.ComponentSpecificationMessage.component_type_version', index=2,
      number=3, type=2, cpp_type=6, label=1,
      has_default_value=True, default_value=1,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='component_name', full_name='android.vts.ComponentSpecificationMessage.component_name', index=3,
      number=4, type=12, cpp_type=9, label=1,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='package', full_name='android.vts.ComponentSpecificationMessage.package', index=4,
      number=11, type=12, cpp_type=9, label=1,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='import', full_name='android.vts.ComponentSpecificationMessage.import', index=5,
      number=12, type=12, cpp_type=9, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='original_data_structure_name', full_name='android.vts.ComponentSpecificationMessage.original_data_structure_name', index=6,
      number=1001, type=12, cpp_type=9, label=1,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='header', full_name='android.vts.ComponentSpecificationMessage.header', index=7,
      number=1002, type=12, cpp_type=9, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='interface', full_name='android.vts.ComponentSpecificationMessage.interface', index=8,
      number=2001, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='attributes', full_name='android.vts.ComponentSpecificationMessage.attributes', index=9,
      number=2101, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=90,
  serialized_end=503,
)

_COMPONENTSPECIFICATIONMESSAGE.fields_by_name['component_class'].enum_type = InterfaceSpecificationMessage_pb2._COMPONENTCLASS
_COMPONENTSPECIFICATIONMESSAGE.fields_by_name['component_type'].enum_type = InterfaceSpecificationMessage_pb2._COMPONENTTYPE
_COMPONENTSPECIFICATIONMESSAGE.fields_by_name['interface'].message_type = InterfaceSpecificationMessage_pb2._INTERFACESPECIFICATIONMESSAGE
_COMPONENTSPECIFICATIONMESSAGE.fields_by_name['attributes'].message_type = InterfaceSpecificationMessage_pb2._VARIABLESPECIFICATIONMESSAGE
DESCRIPTOR.message_types_by_name['ComponentSpecificationMessage'] = _COMPONENTSPECIFICATIONMESSAGE

class ComponentSpecificationMessage(_message.Message):
  __metaclass__ = _reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _COMPONENTSPECIFICATIONMESSAGE

  # @@protoc_insertion_point(class_scope:android.vts.ComponentSpecificationMessage)


# @@protoc_insertion_point(module_scope)
