from pathlib import Path
import re

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate, Frame, PageTemplate, Paragraph, Spacer, Table, TableStyle,
    PageBreak, Preformatted, KeepTogether,
)

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "docs" / "documento-tecnico.md"
OUTPUT = ROOT / "docs" / "entregaveis" / "documento-tecnico.pdf"

pdfmetrics.registerFont(TTFont("Calibri", r"C:\Windows\Fonts\calibri.ttf"))
pdfmetrics.registerFont(TTFont("CalibriBold", r"C:\Windows\Fonts\calibrib.ttf"))

NAVY = colors.HexColor("#172036")
BLUE = colors.HexColor("#1F6FB8")
TEXT = colors.HexColor("#273244")
MUTED = colors.HexColor("#627083")
PAPER = colors.HexColor("#F8F7F3")
LINE = colors.HexColor("#DCE2EB")


def esc(value):
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def inline(value):
    value = esc(value)
    value = re.sub(r"`([^`]+)`", r"<font name='Courier'>\1</font>", value)
    value = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", value)
    return value


styles = getSampleStyleSheet()
styles.add(ParagraphStyle("DocTitle", parent=styles["Title"], fontName="CalibriBold", fontSize=27, leading=31, textColor=NAVY, spaceAfter=9))
styles.add(ParagraphStyle("Subtitle", parent=styles["Normal"], fontName="Calibri", fontSize=12, leading=16, textColor=MUTED, spaceAfter=18))
styles.add(ParagraphStyle("H1", parent=styles["Heading1"], fontName="CalibriBold", fontSize=18, leading=22, textColor=NAVY, spaceBefore=12, spaceAfter=8, borderWidth=0, keepWithNext=True))
styles.add(ParagraphStyle("H2", parent=styles["Heading2"], fontName="CalibriBold", fontSize=13, leading=17, textColor=BLUE, spaceBefore=10, spaceAfter=5, keepWithNext=True))
styles.add(ParagraphStyle("Body", parent=styles["BodyText"], fontName="Calibri", fontSize=9.3, leading=13.4, textColor=TEXT, spaceAfter=6))
styles.add(ParagraphStyle("ListItem", parent=styles["BodyText"], fontName="Calibri", fontSize=9.3, leading=13.4, leftIndent=12, firstLineIndent=-9, textColor=TEXT, spaceAfter=3))
styles.add(ParagraphStyle("CodeBlock", parent=styles["Code"], fontName="Courier", fontSize=7.4, leading=10.2, textColor=NAVY, backColor=colors.HexColor("#EDF3F8"), borderColor=LINE, borderWidth=.4, borderPadding=7, spaceBefore=3, spaceAfter=8))
styles.add(ParagraphStyle("Table", parent=styles["BodyText"], fontName="Calibri", fontSize=7.6, leading=9.4, textColor=TEXT))
styles.add(ParagraphStyle("TableHead", parent=styles["BodyText"], fontName="CalibriBold", fontSize=7.6, leading=9.4, textColor=colors.white))


def page(canvas, doc):
    canvas.saveState()
    canvas.setFillColor(PAPER)
    canvas.rect(0, 0, A4[0], A4[1], fill=1, stroke=0)
    canvas.setFillColor(NAVY)
    canvas.rect(0, A4[1] - 13 * mm, A4[0], 13 * mm, fill=1, stroke=0)
    canvas.setFont("Calibri", 7.5)
    canvas.setFillColor(colors.HexColor("#DCE7F3"))
    canvas.drawString(18 * mm, A4[1] - 8.4 * mm, "SISTEMAS OPERACIONAIS  |  TRABALHO 01")
    canvas.setFillColor(MUTED)
    canvas.setFont("Calibri", 7.5)
    canvas.drawString(18 * mm, 10 * mm, "Simulador de Escalonamento de Processos")
    canvas.drawRightString(A4[0] - 18 * mm, 10 * mm, str(doc.page))
    canvas.restoreState()


def table_from(lines):
    rows = []
    for line in lines:
        cells = [item.strip() for item in line.strip().strip("|").split("|")]
        if not cells or all(re.fullmatch(r"[-: ]+", item) for item in cells):
            continue
        rows.append(cells)
    data = []
    for index, row in enumerate(rows):
        style = styles["TableHead"] if index == 0 else styles["Table"]
        data.append([Paragraph(inline(cell), style) for cell in row])
    widths = [156 * mm / len(data[0])] * len(data[0])
    result = Table(data, colWidths=widths, repeatRows=1, hAlign="LEFT")
    result.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), BLUE),
        ("BACKGROUND", (0, 1), (-1, -1), colors.white),
        ("GRID", (0, 0), (-1, -1), .35, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 4),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    return result


def build_story(markdown):
    lines = markdown.splitlines()
    story = []
    i = 0
    first_title = True
    while i < len(lines):
        line = lines[i]
        if not line.strip():
            i += 1
            continue
        if line.startswith("# "):
            if first_title:
                story.extend([Spacer(1, 30 * mm), Paragraph(inline(line[2:]), styles["DocTitle"]), Paragraph("Trabalho 01 da disciplina CK0234 - Sistemas Operacionais (UFC)", styles["Subtitle"]), Spacer(1, 7 * mm)])
                first_title = False
            else:
                story.append(Paragraph(inline(line[2:]), styles["H1"]))
        elif line.startswith("## "):
            story.append(Paragraph(inline(line[3:]), styles["H1"]))
        elif line.startswith("### "):
            story.append(Paragraph(inline(line[4:]), styles["H2"]))
        elif line.startswith("```"):
            block = []
            i += 1
            while i < len(lines) and not lines[i].startswith("```"):
                block.append(lines[i])
                i += 1
            story.append(Preformatted("\n".join(block), styles["CodeBlock"]))
        elif line.startswith("|"):
            block = []
            while i < len(lines) and lines[i].startswith("|"):
                block.append(lines[i])
                i += 1
            story.extend([table_from(block), Spacer(1, 5)])
            continue
        elif re.match(r"\s*[-*] ", line):
            story.append(Paragraph("• " + inline(re.sub(r"\s*[-*] ", "", line, count=1)), styles["ListItem"]))
        elif re.match(r"\d+\. ", line):
            number, value = line.split(". ", 1)
            story.append(Paragraph(f"{number}. " + inline(value), styles["ListItem"]))
        else:
            story.append(Paragraph(inline(line), styles["Body"]))
        i += 1
    return story


document = BaseDocTemplate(str(OUTPUT), pagesize=A4, leftMargin=18 * mm, rightMargin=18 * mm, topMargin=20 * mm, bottomMargin=17 * mm)
frame = Frame(document.leftMargin, document.bottomMargin, document.width, document.height, id="content")
document.addPageTemplates([PageTemplate(id="A4", frames=[frame], onPage=page)])
document.build(build_story(SOURCE.read_text(encoding="utf-8")))
