"""Offline UML source/export checks. Reads Java source only, never config or event data.

This checks selected class members and exports, not behavioral correctness or full Java syntax.
Sequence order, association semantics, multiplicities and use cases require manual review.
"""
from pathlib import Path
import re
import struct
import sys
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
JAVA = ROOT / 'ai-event-planner/src/main/java'
SOURCES = {p.stem: p for p in JAVA.rglob('*.java')}
EXTERNAL = {'javafx.application.Application'}
errors = []
class_names = set()
member_count = 0
diagram_count = 0


def code_for(name):
    base = name.split('.')[0].split('<')[0]
    path = SOURCES.get(base)
    if path is None:
        return None
    text = path.read_text(encoding='utf-8')
    # Strip comments while preserving string literals, URLs and escaped quotes.
    text = re.sub(r'"(?:\\.|[^"\\])*"|//[^\n]*|/\*[\s\S]*?\*/',
                  lambda m: m[0] if m[0].startswith('"') else '', text)
    return text


for source in sorted(HERE.glob('*.puml')):
    if source.name == 'style.puml':
        continue
    diagram_count += 1
    text = source.read_text(encoding='utf-8')
    if text.count('@startuml') != 1 or text.count('@enduml') != 1:
        errors.append(f'{source.name}: missing/duplicate diagram delimiters')
    if source.name.startswith('class-'):
        current = None
        for line in text.splitlines():
            decl = re.match(r'^(class|interface)\s+(?:"([^"]+)"|(\w+))', line)
            if decl:
                name = decl[2] or decl[3]
                class_names.add(name)
                current = name if '{' in line else None
                if name not in EXTERNAL and code_for(name) is None:
                    errors.append(f'{source.name}: missing Java type {name}')
                continue
            if line.strip() == '}':
                current = None
            if current is None or current in EXTERNAL:
                continue
            member = re.match(r'\s*(?:\{static\}\s*)?([+\-#~]?)\s*(\w+)\s*(\(|:)', line)
            if not member:
                continue
            member_count += 1
            name, kind = member[2], member[3]
            code = code_for(current) or ''
            # Check a declaration, not merely a call in a method body.
            if kind == '(':
                found = re.search(r'(?:\b(?:public|private|protected|static|default|synchronized)\s+)*'
                                  r'[\w.<>,?\[\] ]+\s+' + re.escape(name) + r'\s*\(', code)
            else:
                found = re.search(r'[\w.<>,?\[\] ]+\s+' + re.escape(name) + r'\s*(?:[,;=)])', code)
            if not found:
                errors.append(f'{source.name}: member not found: {current}.{name}')
    svg = source.with_suffix('.svg')
    png = source.with_suffix('.png')
    if not svg.exists() or not png.exists():
        errors.append(f'{source.name}: missing SVG or PNG export')
        continue
    try:
        tree = ET.parse(svg)
        if tree.getroot().tag != '{http://www.w3.org/2000/svg}svg':
            errors.append(f'{svg.name}: root is not SVG')
        content = ''.join(tree.getroot().itertext())
        if 'Syntax Error?' in content or 'An error has occured' in content:
            errors.append(f'{svg.name}: renderer error image')
        data = png.read_bytes()
        if data[:8] != b'\x89PNG\r\n\x1a\n':
            errors.append(f'{png.name}: invalid PNG signature')
        else:
            width, height = struct.unpack('>II', data[16:24])
            if min(width, height) < 100:
                errors.append(f'{png.name}: unexpected dimensions {width}x{height}')
            print(f'{source.stem}: {width}x{height}; SVG + PNG valid')
    except (OSError, ET.ParseError) as error:
        errors.append(f'{source.name}: {error}')

print(f'Checked {diagram_count} diagrams, {len(class_names)} unique declared types, '
      f'{member_count} selected member occurrences.')
if errors:
    print('\n'.join(errors), file=sys.stderr)
    sys.exit(1)
print('PASS: member-name traceability and export structure checks; manual semantic review still required.')
