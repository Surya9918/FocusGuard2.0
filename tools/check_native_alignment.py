"""Check the 64-bit ELF load segments in the built Android package."""

import struct
import sys
import zipfile

PAGE_SIZE = 16 * 1024
PT_LOAD = 1


def check_library(name, data):
    if data[:4] != b"\x7fELF" or data[4] != 2 or data[5] not in (1, 2):
        raise ValueError(f"{name}: expected a 64-bit ELF library")
    order = "<" if data[5] == 1 else ">"
    program_headers = struct.unpack_from(order + "Q", data, 32)[0]
    entry_size, entries = struct.unpack_from(order + "HH", data, 54)
    loads = 0
    for index in range(entries):
        entry = program_headers + index * entry_size
        segment, _, offset, address, _, _, _, alignment = struct.unpack_from(
            order + "IIQQQQQQ", data, entry
        )
        if segment == PT_LOAD:
            loads += 1
            if alignment < PAGE_SIZE or (offset - address) % PAGE_SIZE:
                raise ValueError(f"{name}: load segment {index} is not 16 KB aligned")
    if not loads:
        raise ValueError(f"{name}: no load segments")


def main(path):
    with zipfile.ZipFile(path) as package:
        libraries = [
            name for name in package.namelist()
            if name.endswith(".so") and ("/arm64-v8a/" in name or "/x86_64/" in name)
        ]
        if not any("/arm64-v8a/" in name for name in libraries):
            raise ValueError("No ARM64 native libraries in Android package")
        for name in libraries:
            check_library(name, package.read(name))
        print(f"Verified {len(libraries)} 64-bit libraries")


if __name__ == "__main__":
    main(sys.argv[1])
