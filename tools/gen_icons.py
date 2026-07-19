#!/usr/bin/env python3
"""Generate mipmap PNG fallbacks for ic_launcher (API < 26)."""
import struct, zlib, os

def make_png(width, height, r, g, b):
    """Create a minimal 32-bit RGBA PNG of a solid color."""
    def chunk(ctype, data):
        c = ctype + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xFFFFFFFF)

    header = b'\x89PNG\r\n\x1a\n'
    ihdr = chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))  # 8-bit RGBA

    raw = b''
    for y in range(height):
        raw += b'\x00'  # filter byte
        for x in range(width):
            raw += struct.pack('BBBB', r, g, b, 255)
    idat = chunk(b'IDAT', zlib.compress(raw))
    iend = chunk(b'IEND', b'')
    return header + ihdr + idat + iend

sizes = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

base = '/home/ollama/transcripto-local/app/src/main/res'
green = (0x1B, 0x5E, 0x20)  # primary color

for folder, size in sizes.items():
    os.makedirs(f'{base}/{folder}', exist_ok=True)
    png = make_png(size, size, *green)
    for name in ('ic_launcher.png', 'ic_launcher_round.png'):
        path = f'{base}/{folder}/{name}'
        with open(path, 'wb') as f:
            f.write(png)
        print(f'Created {path} ({size}x{size})')

print('Done.')
