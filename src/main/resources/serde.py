import polyglot
import pickle
import pprint


@polyglot.export_value
def deserialize(payload):
    dump = bytes(payload)
    try:
        obj = pickle.loads(dump)
        return pprint.pformat(obj)
    except TypeError:
        return dump.decode("utf8")
