"""
Generador integral de capturas para Google Play Store - Domino Imperial: Grand Prix v2.5.0
Genera 4 colecciones completas:
1. telefono_1080x2160/ (8 capturas directas ajustadas a ratio 2:1 oficial)
2. telefono_con_fondo_1080x1920/ (8 capturas promocionales con fondo artístico y marco de móvil)
3. tablet_7_pulgadas_1920x1080/ (8 capturas apaisadas 16:9 con fondo temático, textos y marco)
4. tablet_10_pulgadas_2560x1440/ (8 capturas apaisadas 16:9 alta resolución para tablets de 10")
"""

import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

BASE_DIR = r"C:\Users\horts\Documents\Proyectos\DominoGrandPrix"
RELEASE_DIR = os.path.join(BASE_DIR, "PLAY_STORE_RELEASE")
SCREENSHOTS_V25_DIR = os.path.join(RELEASE_DIR, "screenshots_v2.5")

DIR_PHONE_DIRECT = os.path.join(SCREENSHOTS_V25_DIR, "telefono_1080x2160")
DIR_PHONE_PROMO = os.path.join(SCREENSHOTS_V25_DIR, "telefono_con_fondo_1080x1920")
DIR_TABLET_7 = os.path.join(SCREENSHOTS_V25_DIR, "tablet_7_pulgadas_1920x1080")
DIR_TABLET_10 = os.path.join(SCREENSHOTS_V25_DIR, "tablet_10_pulgadas_2560x1440")

for d in [DIR_PHONE_DIRECT, DIR_PHONE_PROMO, DIR_TABLET_7, DIR_TABLET_10]:
    os.makedirs(d, exist_ok=True)

BRAIN_DIR = r"C:\Users\horts\.gemini\antigravity\brain\9956a310-eb79-4b35-99d5-227952cf2a6b"
RES_DIR = os.path.join(BASE_DIR, "app", "src", "main", "res")

# Metadata de las 8 capturas
ITEMS = [
    {
        "id": "01_titulo",
        "filename": "01_titulo",
        "raw_file": os.path.join(BRAIN_DIR, "raw_01_titulo.png"),
        "bg_file": os.path.join(RES_DIR, "drawable", "bg_cover.jpg"),
        "badge": "GRAND PRIX MUNDIAL",
        "title": "TORNEO MUNDIAL DE DOMINÓ",
        "subtitle": "Gira mundial, partida rápida y desafíos diarios en solitario y sin conexión.",
        "bullets": [
            ("7 Sedes Legendarias", "Recorre México, La Habana, San Juan, Tokio y más."),
            ("100% Sin Conexión", "Juega en cualquier lugar sin necesidad de internet."),
            ("Sin Anuncios ni Compras", "Experiencia de juego limpia, fluida y completa.")
        ]
    },
    {
        "id": "02_mesa_juego",
        "filename": "02_mesa_juego",
        "raw_file": os.path.join(BRAIN_DIR, "raw_02_mesa_3d.png"),
        "bg_file": os.path.join(RES_DIR, "drawable", "bg_table.jpg"),
        "badge": "TABLERO TÁCTIL INTUITIVO",
        "title": "MESA DE JUEGO Y CONTROL TÁCTIL",
        "subtitle": "Cadena en serpentina fluida, rivales con IA avanzada y cámara 2D / 3D.",
        "bullets": [
            ("Cámara Dual", "Alterna entre vista 2D cenital y 3D en perspectiva."),
            ("Ayudas Tácticas", "Botones flotantes de Pista y Deshacer jugada."),
            ("Rivales Inteligentes", "3 niveles de estrategia adaptativa.")
        ]
    },
    {
        "id": "03_contador_fichas",
        "filename": "03_contador_fichas",
        "raw_file": os.path.join(BRAIN_DIR, "raw_03_contador_fichas.png"),
        "bg_file": os.path.join(RES_DIR, "drawable-nodpi", "venue_mexico.jpg"),
        "badge": "ESTRATEGIA AVANZADA",
        "title": "CONTADOR DE FICHAS Y TÁCTICA",
        "subtitle": "Visualiza al instante las fichas restantes por palo para planear tu jugada.",
        "bullets": [
            ("Conteo en Tiempo Real", "Seguimiento automático de fichas jugadas y por salir."),
            ("Desglose por Palo", "De blancas a seises con un solo toque."),
            ("Herramienta de Maestría", "Domina el tablero leyendo las posibilidades del rival.")
        ]
    },
    {
        "id": "04_reglas_modalidades",
        "filename": "04_reglas_modalidades",
        "raw_file": os.path.join(BRAIN_DIR, "raw_09_reglas.png"),
        "bg_file": os.path.join(RES_DIR, "drawable-nodpi", "venue_havana.jpg"),
        "badge": "REGLAMENTO INTERNACIONAL",
        "title": "8 MODALIDADES OFICIALES",
        "subtitle": "Draw, Block, All Fives, All Threes, Fives & Threes, Matador y más.",
        "bullets": [
            ("Reglas Tradicionales", "Dominó Clásico con robo, Tranca y Bloqueo puro."),
            ("Modalidades por Puntos", "All Fives, All Threes y Fives & Threes con tanteo en vivo."),
            ("Variantes Especiales", "Matador con suma 7 y Block Hungarian con encadenamiento.")
        ]
    },
    {
        "id": "05_sede_mexico",
        "filename": "05_sede_mexico",
        "raw_file": os.path.join(BRAIN_DIR, "raw_05_sede_mexico.png"),
        "bg_file": os.path.join(RES_DIR, "drawable-nodpi", "venue_sanjuan.jpg"),
        "badge": "CIRCUITO MUNDIAL",
        "title": "GIRA EN 7 SEDES LEGENDARIAS",
        "subtitle": "México, La Habana, San Juan, Nueva Orleans, Miami, Las Vegas y Tokio.",
        "bullets": [
            ("35 Retos Progresivos", "Supera 5 desafíos únicos en cada ciudad del mundo."),
            ("Música Tradicional", "Banda sonora ambiental temática para cada localidad."),
            ("Atmósfera Inmersiva", "Postales y diseño visual fiel a cada sede.")
        ]
    },
    {
        "id": "06_partida_rapida",
        "filename": "06_partida_rapida",
        "raw_file": os.path.join(BRAIN_DIR, "raw_06_partida_rapida.png"),
        "bg_file": os.path.join(RES_DIR, "drawable-nodpi", "venue_new_orleans.jpg"),
        "badge": "JUEGA A TU MANERA",
        "title": "PARTIDA RÁPIDA PERSONALIZADA",
        "subtitle": "Configura modalidad, meta de puntos, 2 o 4 jugadores, individual o 2v2.",
        "bullets": [
            ("Configuración Total", "Elige meta desde 30 hasta 150 puntos y dificultad de IA."),
            ("Pasa el Móvil", "Opción para 2 humanos en el mismo dispositivo."),
            ("Formato Parejas", "Modo 2v2 para disfrutar la complicidad táctica del dominó.")
        ]
    },
    {
        "id": "07_logros",
        "filename": "07_logros",
        "raw_file": os.path.join(BRAIN_DIR, "raw_07_logros.png"),
        "bg_file": os.path.join(RES_DIR, "drawable-nodpi", "venue_las_vegas.jpg"),
        "badge": "SUPERACIÓN CONSTANTE",
        "title": "DESAFÍOS Y LOGROS DEL CIRCUITO",
        "subtitle": "Colecciona triunfos, victorias sin robar y hazañas en todas las sedes.",
        "bullets": [
            ("Metas de Carrera", "Desbloquea logros por victorias, cierres y partidas limpias."),
            ("Estadísticas Completas", "Historial de partidas jugadas, porcentaje y récords."),
            ("Progreso Permanente", "Tus hazañas guardadas localmente en tu dispositivo.")
        ]
    },
    {
        "id": "08_vitrina_trofeos",
        "filename": "08_vitrina_trofeos",
        "raw_file": os.path.join(BRAIN_DIR, "raw_08_vitrina.png"),
        "bg_file": os.path.join(RES_DIR, "drawable-nodpi", "venue_tokyo.jpg"),
        "badge": "COLECCIÓN IMPERIAL",
        "title": "VITRINA DE TROFEOS DE LA GIRA",
        "subtitle": "Conquista cada ciudad para coronarte como el Campeón Imperial del Dominó.",
        "bullets": [
            ("7 Trofeos de Sede", "Copas y galardones únicos de cada ciudad del circuito."),
            ("Sala de Honor", "Exposición permanente de tus triunfos en la gira."),
            ("El Gran Trofeo Imperial", "El premio supremo reservado al verdadero maestro.")
        ]
    }
]

# Assets comunes
LOGO_PATH = os.path.join(RES_DIR, "drawable-nodpi", "app_logo_icon.png")
LOGO_IMG = Image.open(LOGO_PATH).convert("RGBA")

# Fuentes estándar Windows
FONT_ARIAL_BOLD = r"C:\Windows\Fonts\arialbd.ttf"
FONT_ARIAL_REG = r"C:\Windows\Fonts\arial.ttf"
FONT_EMOJI = r"C:\Windows\Fonts\seguiemj.ttf"


def generate_phone_direct(item):
    """
    Genera captura directa a 1080x2160 (exactamente 2:1, formato Play Store para móviles).
    Escala la captura 1080x2400 para que se visualice completa y nítida sin cortar ni el HUD ni la mano.
    """
    raw = Image.open(item["raw_file"]).convert("RGBA")
    # Escala completa a altura 2160 (ancho 972)
    target_h = 2160
    target_w = int(1080 * target_h / 2400) # 972
    scaled = raw.resize((target_w, target_h), Image.Resampling.LANCZOS)
    
    # Crear canvas 1080x2160 con fondo oscuro y sutil barra dorada lateral
    canvas = Image.new("RGBA", (1080, 2160), (12, 16, 24, 255))
    x_off = (1080 - target_w) // 2 # 54px
    
    # Fondo con textura sutil en los laterales
    bg_art = Image.open(item["bg_file"]).convert("RGB")
    ratio = max(1080 / bg_art.width, 2160 / bg_art.height)
    bg_art_res = bg_art.resize((int(bg_art.width * ratio), int(bg_art.height * ratio)), Image.Resampling.LANCZOS)
    bx = (bg_art_res.width - 1080) // 2
    by = (bg_art_res.height - 2160) // 2
    bg_side = bg_art_res.crop((bx, by, bx + 1080, by + 2160)).filter(ImageFilter.GaussianBlur(radius=15))
    
    # Oscurecer fondo lateral
    dark = Image.new("RGBA", (1080, 2160), (10, 14, 20, 210))
    bg_side_dark = Image.alpha_composite(bg_side.convert("RGBA"), dark)
    canvas.alpha_composite(bg_side_dark, (0, 0))
    
    # Pegar captura centrada
    canvas.alpha_composite(scaled, (x_off, 0))
    
    # Finas líneas doradas separadoras en los laterales de la pantalla
    draw = ImageDraw.Draw(canvas)
    draw.line([(x_off - 1, 0), (x_off - 1, 2160)], fill=(218, 165, 32, 180), width=1)
    draw.line([(x_off + target_w, 0), (x_off + target_w, 2160)], fill=(218, 165, 32, 180), width=1)
    
    out_path = os.path.join(DIR_PHONE_DIRECT, f"{item['filename']}_1080x2160.png")
    canvas.convert("RGB").save(out_path, quality=95)
    
    # Guardar también en el root de screenshots_v2.5
    root_path = os.path.join(SCREENSHOTS_V25_DIR, f"{item['filename']}_1080x2160.png")
    canvas.convert("RGB").save(root_path, quality=95)
    print(f"[Direct 1080x2160] Guardado: {out_path}")


def generate_phone_promo(item):
    """
    Genera captura promocional 1080x1920 (9:16) con fondo temático, textos y marco de smartphone.
    """
    W, H = 1080, 1920
    # Cargar fondo
    bg = Image.open(item["bg_file"]).convert("RGB")
    ratio = max(W / bg.width, H / bg.height)
    bg_res = bg.resize((int(bg.width * ratio), int(bg.height * ratio)), Image.Resampling.LANCZOS)
    bx = (bg_res.width - W) // 2
    by = (bg_res.height - H) // 2
    bg_cropped = bg_res.crop((bx, by, bx + W, by + H))
    bg_blur = bg_cropped.filter(ImageFilter.GaussianBlur(radius=10))
    
    # Overlay con gradiente
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw_ov = ImageDraw.Draw(overlay)
    for y in range(H):
        if y < 420:
            alpha = int(225 - 40 * (y / 420.0))
        elif y > 1650:
            alpha = int(185 + 70 * ((y - 1650) / 270.0))
        else:
            alpha = 180
        draw_ov.line([(0, y), (W, y)], fill=(11, 16, 26, alpha))
        
    canvas = Image.alpha_composite(bg_blur.convert("RGBA"), overlay)
    
    # Borde dorado tenue
    draw = ImageDraw.Draw(canvas)
    draw.rectangle([14, 14, W - 14, H - 14], outline=(218, 165, 32, 120), width=2)
    
    # Tipografías
    font_badge = ImageFont.truetype(FONT_ARIAL_BOLD, 20)
    font_title = ImageFont.truetype(FONT_ARIAL_BOLD, 42)
    font_sub = ImageFont.truetype(FONT_ARIAL_REG, 23)
    
    # Badge superior
    badge_len = draw.textlength(item["badge"], font=font_badge)
    pw = int(badge_len + 50)
    px = (W - pw) // 2
    draw.rounded_rectangle([px, 48, px + pw, 92], radius=14, fill=(20, 28, 42, 230), outline=(218, 165, 32), width=2)
    draw.text((px + 25, 59), item["badge"], fill=(255, 215, 0), font=font_badge)
    
    # Título principal
    t_len = draw.textlength(item["title"], font=font_title)
    draw.text(((W - t_len) / 2, 114), item["title"], fill=(255, 255, 255), font=font_title)
    
    # Subtítulo
    s_len = draw.textlength(item["subtitle"], font=font_sub)
    if s_len > W - 80:
        # Repartir en 2 líneas si es largo
        words = item["subtitle"].split()
        mid = len(words) // 2
        l1 = " ".join(words[:mid])
        l2 = " ".join(words[mid:])
        l1_len = draw.textlength(l1, font=font_sub)
        l2_len = draw.textlength(l2, font=font_sub)
        draw.text(((W - l1_len) / 2, 172), l1, fill=(203, 213, 225), font=font_sub)
        draw.text(((W - l2_len) / 2, 202), l2, fill=(203, 213, 225), font=font_sub)
        phone_top = 265
    else:
        draw.text(((W - s_len) / 2, 178), item["subtitle"], fill=(203, 213, 225), font=font_sub)
        phone_top = 245
        
    # Marco de Smartphone
    screen_raw = Image.open(item["raw_file"]).convert("RGBA")
    dev_h = H - phone_top - 60 # aprox 1615px
    dev_w = int(dev_h * (1080 / 2400)) # aprox 726px
    screen_resized = screen_raw.resize((dev_w, dev_h), Image.Resampling.LANCZOS)
    
    bezel = 14
    pw = dev_w + bezel * 2
    ph = dev_h + bezel * 2
    phone = Image.new("RGBA", (pw, ph), (0, 0, 0, 0))
    p_draw = ImageDraw.Draw(phone)
    
    # Chasis exterior
    p_draw.rounded_rectangle([0, 0, pw, ph], radius=44, fill=(18, 22, 30, 255), outline=(218, 165, 32), width=3)
    p_draw.rounded_rectangle([bezel - 2, bezel - 2, pw - bezel + 2, ph - bezel + 2], radius=34, outline=(50, 60, 75), width=2)
    
    # Máscara para la pantalla
    s_mask = Image.new("L", (dev_w, dev_h), 0)
    s_mask_draw = ImageDraw.Draw(s_mask)
    s_mask_draw.rounded_rectangle([0, 0, dev_w, dev_h], radius=32, fill=255)
    phone.paste(screen_resized, (bezel, bezel), s_mask)
    
    # Sombra
    shadow = Image.new("RGBA", (pw + 80, ph + 80), (0, 0, 0, 0))
    sh_draw = ImageDraw.Draw(shadow)
    sh_draw.rounded_rectangle([30, 30, pw + 50, ph + 50], radius=55, fill=(0, 0, 0, 190))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=22))
    
    pos_x = (W - pw) // 2
    pos_y = phone_top
    
    canvas.alpha_composite(shadow, (pos_x - 40, pos_y - 25))
    canvas.alpha_composite(phone, (pos_x, pos_y))
    
    out_path = os.path.join(DIR_PHONE_PROMO, f"{item['filename']}_1080x1920.png")
    canvas.convert("RGB").save(out_path, quality=95)
    print(f"[Promo 1080x1920] Guardado: {out_path}")


def generate_tablet(item, width, height, out_dir, suffix):
    """
    Genera captura para Tablet apaisada 16:9 (1920x1080 para 7" o 2560x1440 para 10").
    Incluye fondo del juego con desenfoque suave, columna izquierda de branding y características,
    y dispositivo enmarcado en la derecha con bordes dorados y sombra.
    """
    scale = width / 1920.0
    
    # Cargar y adaptar fondo
    bg = Image.open(item["bg_file"]).convert("RGB")
    ratio = max(width / bg.width, height / bg.height)
    bg_res = bg.resize((int(bg.width * ratio), int(bg.height * ratio)), Image.Resampling.LANCZOS)
    bx = (bg_res.width - width) // 2
    by = (bg_res.height - height) // 2
    bg_cropped = bg_res.crop((bx, by, bx + width, by + height))
    bg_blur = bg_cropped.filter(ImageFilter.GaussianBlur(radius=int(10 * scale)))
    
    # Overlay con gradiente horizontal (más oscuro a la izquierda para máxima legibilidad)
    overlay = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw_ov = ImageDraw.Draw(overlay)
    for x in range(width):
        factor = 1.0 - (x / float(width))
        alpha = int(145 + 85 * (factor ** 0.8))
        draw_ov.line([(x, 0), (x, height)], fill=(11, 16, 26, alpha))
        
    canvas = Image.alpha_composite(bg_blur.convert("RGBA"), overlay)
    
    # Marco dorado sutil alrededor de todo el arte
    b_draw = ImageDraw.Draw(canvas)
    m1 = int(20 * scale)
    m2 = int(24 * scale)
    b_draw.rectangle([m1, m1, width - m1, height - m1], outline=(218, 165, 32, 140), width=int(2 * scale))
    b_draw.rectangle([m2, m2, width - m2, height - m2], outline=(255, 215, 0, 70), width=int(1 * scale))
    
    # Tipografías
    font_logo_title = ImageFont.truetype(FONT_ARIAL_BOLD, int(22 * scale))
    font_logo_sub = ImageFont.truetype(FONT_ARIAL_REG, int(18 * scale))
    font_badge = ImageFont.truetype(FONT_ARIAL_BOLD, int(19 * scale))
    font_title = ImageFont.truetype(FONT_ARIAL_BOLD, int(44 * scale))
    font_sub = ImageFont.truetype(FONT_ARIAL_REG, int(24 * scale))
    font_bullet_b = ImageFont.truetype(FONT_ARIAL_BOLD, int(21 * scale))
    font_bullet = ImageFont.truetype(FONT_ARIAL_REG, int(21 * scale))
    font_bottom = ImageFont.truetype(FONT_ARIAL_BOLD, int(19 * scale))
    
    draw = ImageDraw.Draw(canvas)
    
    # Logo del juego
    logo_size = int(96 * scale)
    logo_res = LOGO_IMG.resize((logo_size, logo_size), Image.Resampling.LANCZOS)
    canvas.alpha_composite(logo_res, (int(90 * scale), int(80 * scale)))
    
    # Texto junto al logo
    draw.text((int(205 * scale), int(95 * scale)), "DOMINO IMPERIAL", fill=(255, 215, 0), font=font_logo_title)
    draw.text((int(205 * scale), int(130 * scale)), "GRAND PRIX MUNDIAL • v2.5", fill=(160, 174, 192), font=font_logo_sub)
    
    # Badge / Pill
    badge_len = draw.textlength(item["badge"], font=font_badge)
    pw = int(badge_len + 45 * scale)
    pill_box = [int(90 * scale), int(215 * scale), int(90 * scale) + pw, int(260 * scale)]
    draw.rounded_rectangle(pill_box, radius=int(12 * scale), fill=(30, 41, 59, 210), outline=(218, 165, 32), width=int(2 * scale))
    draw.text((int(112 * scale), int(225 * scale)), item["badge"], fill=(255, 215, 0), font=font_badge)
    
    # Título principal (dividido si es largo)
    title_words = item["title"].split()
    if len(title_words) > 3 and " Y " in item["title"]:
        parts = item["title"].split(" Y ")
        t_line1 = parts[0] + " Y"
        t_line2 = parts[1]
        draw.text((int(90 * scale), int(285 * scale)), t_line1, fill=(255, 255, 255), font=font_title)
        draw.text((int(90 * scale), int(345 * scale)), t_line2, fill=(255, 255, 255), font=font_title)
        sub_y = int(425 * scale)
    else:
        draw.text((int(90 * scale), int(290 * scale)), item["title"], fill=(255, 255, 255), font=font_title)
        sub_y = int(375 * scale)
        
    # Subtítulo (en 2 líneas si es largo)
    sub_words = item["subtitle"].split()
    mid = len(sub_words) // 2
    s1 = " ".join(sub_words[:mid])
    s2 = " ".join(sub_words[mid:])
    draw.text((int(90 * scale), sub_y), s1, fill=(203, 213, 225), font=font_sub)
    draw.text((int(90 * scale), sub_y + int(36 * scale)), s2, fill=(203, 213, 225), font=font_sub)
    
    # Bullets de características
    by = sub_y + int(100 * scale)
    for b_title, b_desc in item["bullets"]:
        # Icono dorado con check
        draw.ellipse([int(90 * scale), by + int(3 * scale), int(114 * scale), by + int(27 * scale)], fill=(218, 165, 32))
        try:
            draw.text((int(98 * scale), by + int(3 * scale)), "✔", fill=(15, 23, 42), font=ImageFont.truetype(FONT_EMOJI, int(15 * scale)))
        except:
            pass
        b_prefix = b_title + ": "
        draw.text((int(130 * scale), by + int(2 * scale)), b_prefix, fill=(255, 215, 0), font=font_bullet_b)
        b_len = draw.textlength(b_prefix, font=font_bullet_b)
        draw.text((int(130 * scale) + b_len, by + int(2 * scale)), b_desc, fill=(241, 245, 249), font=font_bullet)
        by += int(56 * scale)
        
    # Barra inferior informativa
    bot_y1 = int(935 * scale)
    bot_y2 = int(990 * scale)
    draw.rounded_rectangle([int(90 * scale), bot_y1, int(860 * scale), bot_y2], radius=int(14 * scale), fill=(15, 23, 42, 220), outline=(100, 116, 139), width=1)
    draw.text((int(120 * scale), bot_y1 + int(16 * scale)), "100% OFFLINE • SIN ANUNCIOS • 8 MODALIDADES", fill=(255, 215, 0), font=font_bottom)
    
    # Dispositivo enmarcado a la derecha
    target_h = int(920 * scale)
    target_w = int(target_h * (1080 / 2400.0)) # aprox 414px at 1920
    screen_raw = Image.open(item["raw_file"]).convert("RGBA")
    screen_resized = screen_raw.resize((target_w, target_h), Image.Resampling.LANCZOS)
    
    bezel = int(14 * scale)
    pw = target_w + bezel * 2
    ph = target_h + bezel * 2
    device = Image.new("RGBA", (pw, ph), (0, 0, 0, 0))
    dev_draw = ImageDraw.Draw(device)
    
    # Chasis de la tablet/móvil
    dev_draw.rounded_rectangle([0, 0, pw, ph], radius=int(32 * scale), fill=(18, 22, 30, 255), outline=(218, 165, 32), width=int(3 * scale))
    dev_draw.rounded_rectangle([bezel - 2, bezel - 2, pw - bezel + 2, ph - bezel + 2], radius=int(26 * scale), outline=(60, 70, 90), width=int(2 * scale))
    
    # Máscara para la pantalla
    mask = Image.new("L", (target_w, target_h), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle([0, 0, target_w, target_h], radius=int(24 * scale), fill=255)
    device.paste(screen_resized, (bezel, bezel), mask)
    
    # Sombra
    shadow = Image.new("RGBA", (pw + int(60 * scale), ph + int(60 * scale)), (0, 0, 0, 0))
    sh_draw = ImageDraw.Draw(shadow)
    sh_draw.rounded_rectangle([int(20 * scale), int(20 * scale), pw + int(40 * scale), ph + int(40 * scale)], radius=int(40 * scale), fill=(0, 0, 0, 190))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=int(18 * scale)))
    
    dev_x = width - pw - int(120 * scale)
    dev_y = (height - ph) // 2
    
    canvas.alpha_composite(shadow, (dev_x - int(30 * scale), dev_y - int(20 * scale)))
    canvas.alpha_composite(device, (dev_x, dev_y))
    
    out_path = os.path.join(out_dir, f"{item['filename']}_{suffix}.png")
    canvas.convert("RGB").save(out_path, quality=95)
    print(f"[{suffix}] Guardado: {out_path}")


def main():
    print("Iniciando generación de capturas para Google Play Store...")
    for idx, item in enumerate(ITEMS, 1):
        print(f"\n--- Procesando {idx}/8: {item['filename']} ---")
        generate_phone_direct(item)
        generate_phone_promo(item)
        generate_tablet(item, 1920, 1080, DIR_TABLET_7, "1920x1080")
        generate_tablet(item, 2560, 1440, DIR_TABLET_10, "2560x1440")
    print("\n¡Todas las capturas se generaron exitosamente!")

if __name__ == "__main__":
    main()
