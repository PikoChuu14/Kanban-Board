"""Generate Windows and browser icon files from the canonical FlowOps mark."""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent.parent
MASTER_SIZE = 1024
DESIGN_SIZE = 512


def point(value: float) -> float:
    return value * MASTER_SIZE / DESIGN_SIZE


def bezier(start, control1, control2, end, steps=40):
    points = []
    for index in range(1, steps + 1):
        t = index / steps
        inverse = 1 - t
        x = (
            inverse**3 * start[0]
            + 3 * inverse**2 * t * control1[0]
            + 3 * inverse * t**2 * control2[0]
            + t**3 * end[0]
        )
        y = (
            inverse**3 * start[1]
            + 3 * inverse**2 * t * control1[1]
            + 3 * inverse * t**2 * control2[1]
            + t**3 * end[1]
        )
        points.append((x, y))
    return points


def scaled(points):
    return [(point(x), point(y)) for x, y in points]


def circle(draw, center, radius, fill, outline=None, width=1):
    x, y = scaled([center])[0]
    r = point(radius)
    draw.ellipse(
        (x - r, y - r, x + r, y + r),
        fill=fill,
        outline=outline,
        width=max(1, round(point(width))),
    )


def create_master():
    image = Image.new("RGBA", (MASTER_SIZE, MASTER_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    navy = "#12344c"
    cyan = "#57c3d2"
    white = "#f8fbff"

    draw.rounded_rectangle(
        (0, 0, MASTER_SIZE - 1, MASTER_SIZE - 1),
        radius=point(112),
        fill=navy,
    )

    path = [(139, 165), (240, 165)]
    path += bezier((240, 165), (278, 165), (298, 187), (298, 222))
    path.append((298, 290))
    path += bezier((298, 290), (298, 325), (317, 347), (356, 347))
    path.append((384, 347))
    line = scaled(path)
    line_width = round(point(43))
    # Fill every sampled point with a round brush. Pillow's polygon-based line
    # joins can otherwise leave wedges along a thick Bézier stroke.
    draw.line(line, fill=cyan, width=line_width)
    cap_radius = line_width / 2
    for x, y in line:
        draw.ellipse(
            (x - cap_radius, y - cap_radius, x + cap_radius, y + cap_radius),
            fill=cyan,
        )

    circle(draw, (139, 165), 45, white)
    circle(draw, (298, 256), 45, cyan, outline=navy, width=21)
    circle(draw, (384, 347), 45, white)
    return image


def main():
    master = create_master()
    public = ROOT / "frontend" / "public"
    installer = ROOT / "installer"
    public.mkdir(parents=True, exist_ok=True)
    installer.mkdir(parents=True, exist_ok=True)

    icon_sizes = [(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)]
    for destination in (public / "favicon.ico", installer / "FlowOps.ico"):
        master.save(destination, format="ICO", sizes=icon_sizes)

    for size in (192, 512):
        resized = master.resize((size, size), Image.Resampling.LANCZOS)
        resized.save(public / f"flowops-icon-{size}.png", optimize=True)


if __name__ == "__main__":
    main()
