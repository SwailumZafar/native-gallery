import os
import sys
import re
import time
import socket
import urllib.parse
import http.server
import io
import psutil

try:
    import qrcode
    from qrcode.image.svg import SvgPathImage
    HAS_QRCODE = True
except ImportError:
    HAS_QRCODE = False

PORT = 8080
DIRECTORY = os.path.dirname(os.path.abspath(__file__))

def cleanup_old_instances():
    """Kill any previous serve_apk.py processes holding the ports."""
    my_pid = os.getpid()
    for proc in psutil.process_iter(['pid', 'name', 'cmdline']):
        try:
            pid = proc.info['pid']
            if pid == my_pid:
                continue
            name = (proc.info['name'] or '').lower()
            cmdline = " ".join(proc.info['cmdline'] or [])
            if 'python' in name and 'serve_apk.py' in cmdline:
                print(f"Terminating old server instance (PID {pid})...", flush=True)
                proc.terminate()
                try:
                    proc.wait(timeout=2)
                except psutil.TimeoutExpired:
                    proc.kill()
        except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
            pass

def get_app_version():
    gradle_path = os.path.join(DIRECTORY, "app", "build.gradle.kts")
    if os.path.exists(gradle_path):
        try:
            with open(gradle_path, "r", encoding="utf-8") as f:
                content = f.read()
                m = re.search(r'versionName\s*=\s*"([^"]+)"', content)
                if m:
                    return m.group(1)
        except Exception:
            pass
    return "0.9.0"

VERSION = get_app_version()
PUBLIC_FILENAME = f"NativeGallery-v{VERSION}.apk"

def get_primary_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 80))
        return s.getsockname()[0]
    except Exception:
        return "127.0.0.1"
    finally:
        s.close()

def generate_qr_svg(data_url):
    if not HAS_QRCODE:
        return ""
    try:
        qr = qrcode.QRCode(
            version=None,
            error_correction=qrcode.constants.ERROR_CORRECT_M,
            box_size=8,
            border=2,
        )
        qr.add_data(data_url)
        qr.make(fit=True)
        img = qr.make_image(image_factory=SvgPathImage)
        buf = io.BytesIO()
        img.save(buf)
        svg_str = buf.getvalue().decode("utf-8")
        svg_str = re.sub(r"<\?xml.*?\?>", "", svg_str).strip()
        return svg_str
    except Exception as e:
        print(f"Error generating SVG QR: {e}", flush=True)
        return ""

class ApkServerHandler(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, format, *args):
        client = self.client_address[0] if self.client_address else "unknown"
        print(f"[{client}] {format % args}", flush=True)

    def get_apk_path(self, requested_filename=None):
        if requested_filename:
            candidate = os.path.join(DIRECTORY, os.path.basename(requested_filename))
            if os.path.exists(candidate):
                return candidate
        candidates = [
            os.path.join(DIRECTORY, PUBLIC_FILENAME),
            os.path.join(DIRECTORY, "app", "build", "outputs", "apk", "debug", "app-debug.apk"),
            os.path.join(DIRECTORY, "app-debug.apk"),
        ]
        for c in candidates:
            if os.path.exists(c):
                return c
        return candidates[0]

    def send_cors_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Range, Content-Type")

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_cors_headers()
        self.send_header("Content-Length", "0")
        self.end_headers()

    def do_HEAD(self):
        parsed = urllib.parse.urlparse(self.path)
        clean_path = parsed.path

        if clean_path in ("/app-debug.apk", f"/{PUBLIC_FILENAME}", "/download", "/apk") or clean_path.endswith(".apk"):
            req_file = clean_path.lstrip("/") if clean_path.endswith(".apk") else None
            apk_path = self.get_apk_path(req_file)
            if not os.path.exists(apk_path):
                self.send_error(404, "APK file not found on server")
                return
            size = os.path.getsize(apk_path)
            pub_download_name = os.path.basename(apk_path)
            self.send_response(200)
            self.send_cors_headers()
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", f'attachment; filename="{pub_download_name}"')
            self.send_header("Content-Length", str(size))
            self.send_header("Accept-Ranges", "bytes")
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
            self.end_headers()
        else:
            self.send_response(200)
            self.send_cors_headers()
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Cache-Control", "no-cache")
            self.end_headers()

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        clean_path = parsed.path

        if clean_path in ("/app-debug.apk", f"/{PUBLIC_FILENAME}", "/download", "/apk") or clean_path.endswith(".apk"):
            req_file = clean_path.lstrip("/") if clean_path.endswith(".apk") else None
            apk_path = self.get_apk_path(req_file)
            if not os.path.exists(apk_path):
                self.send_error(404, "APK file not found on server")
                return
            size = os.path.getsize(apk_path)
            self.serve_apk_file(apk_path, size)
            return

        if clean_path == "/health":
            body = b'{"status":"ok","ready":true}'
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Connection", "close")
            self.close_connection = True
            self.end_headers()
            self.wfile.write(body)
            self.wfile.flush()
            return

        apk_path = self.get_apk_path()
        size = os.path.getsize(apk_path) if os.path.exists(apk_path) else 0
        self.serve_html_page(size)

    def serve_html_page(self, size):
        primary_ip = get_primary_ip()
        host = self.headers.get("Host", f"{primary_ip}:{PORT}")
        download_url = f"http://{host}/{PUBLIC_FILENAME}"
        qr_svg = generate_qr_svg(download_url)
        size_mb = size / (1024 * 1024)

        html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Native Gallery v{VERSION} - WiFi Install</title>
    <style>
        * {{ box-sizing: border-box; margin: 0; padding: 0; }}
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            background: #090D11;
            color: #E6E8EC;
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            padding: 24px 16px;
        }}
        .card {{
            background: rgba(16, 23, 29, 0.96);
            border: 1px solid rgba(50, 220, 198, 0.3);
            border-radius: 28px;
            padding: 36px 24px;
            text-align: center;
            max-width: 440px;
            width: 100%;
            box-shadow: 0 20px 50px rgba(0, 0, 0, 0.8), 0 0 30px rgba(50, 220, 198, 0.12);
        }}
        .badge {{
            display: inline-block;
            background: rgba(50, 220, 198, 0.15);
            color: #32DCC6;
            font-size: 13px;
            font-weight: 800;
            padding: 6px 16px;
            border-radius: 20px;
            margin-bottom: 16px;
            letter-spacing: 0.8px;
            text-transform: uppercase;
        }}
        h1 {{
            font-size: 28px;
            font-weight: 800;
            color: #FFFFFF;
            margin-bottom: 6px;
            letter-spacing: -0.5px;
        }}
        .subtitle {{
            color: #32DCC6;
            font-size: 14px;
            font-weight: 600;
            margin-bottom: 24px;
        }}
        .btn {{
            display: flex;
            align-items: center;
            justify-content: center;
            background: linear-gradient(135deg, #32DCC6 0%, #008F83 100%);
            color: #071210;
            text-decoration: none;
            font-weight: 800;
            font-size: 17px;
            padding: 18px 24px;
            border-radius: 18px;
            box-shadow: 0 6px 20px rgba(50, 220, 198, 0.35);
            transition: transform 0.1s ease, box-shadow 0.1s ease;
            cursor: pointer;
            border: none;
            width: 100%;
        }}
        .btn:hover {{
            transform: translateY(-1px);
            box-shadow: 0 8px 28px rgba(50, 220, 198, 0.5);
        }}
        .btn:active {{
            transform: scale(0.97);
        }}
        .meta {{
            margin-top: 14px;
            font-size: 13px;
            color: #8B949E;
        }}
        .meta strong {{
            color: #E6E8EC;
        }}
        .direct-link-box {{
            margin-top: 12px;
            padding: 10px 14px;
            background: rgba(255, 255, 255, 0.04);
            border: 1px solid rgba(255, 255, 255, 0.08);
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 12px;
            color: #8B949E;
            word-break: break-all;
        }}
        .direct-link-box a {{
            color: #32DCC6;
            text-decoration: none;
            font-weight: 600;
        }}
        .copy-btn {{
            background: rgba(50, 220, 198, 0.15);
            color: #32DCC6;
            border: 1px solid rgba(50, 220, 198, 0.3);
            border-radius: 8px;
            padding: 4px 10px;
            font-size: 11px;
            font-weight: 700;
            cursor: pointer;
            margin-left: 8px;
            white-space: nowrap;
        }}
        .qr-section {{
            margin-top: 24px;
            padding-top: 20px;
            border-top: 1px solid rgba(255, 255, 255, 0.08);
            display: flex;
            flex-direction: column;
            align-items: center;
        }}
        .qr-label {{
            font-size: 13px;
            font-weight: 600;
            color: #8B949E;
            margin-bottom: 12px;
        }}
        .qr-container {{
            background: #FFFFFF;
            padding: 12px;
            border-radius: 18px;
            display: inline-block;
        }}
        .qr-container svg {{
            display: block;
            width: 190px;
            height: 190px;
        }}
        .features {{
            margin-top: 24px;
            padding: 16px;
            background: rgba(50, 220, 198, 0.05);
            border: 1px solid rgba(50, 220, 198, 0.15);
            border-radius: 16px;
            text-align: left;
            font-size: 13px;
        }}
        .features .title {{
            color: #32DCC6;
            font-weight: 700;
            margin-bottom: 8px;
            display: block;
        }}
        .features ul {{
            list-style: none;
            padding-left: 0;
        }}
        .features li {{
            margin-bottom: 6px;
            color: #C9D1D9;
            line-height: 1.4;
        }}
        .instructions {{
            margin-top: 20px;
            text-align: left;
            font-size: 12px;
            color: #8B949E;
            line-height: 1.5;
        }}
        .instructions ol {{
            padding-left: 18px;
            margin-top: 6px;
        }}
        .instructions li {{
            margin-bottom: 4px;
        }}
    </style>
</head>
<body>
    <div class="card">
        <div class="badge">ColorOS 17 Living Flow &bull; v{VERSION}</div>
        <h1>Native Gallery</h1>
        <div class="subtitle">Version {VERSION} &bull; ColorOS 17 Living Flow &bull; 60/90/120Hz</div>

        <a class="btn" href="/{PUBLIC_FILENAME}" download="{PUBLIC_FILENAME}">
            ⬇️ Download &amp; Install APK
        </a>

        <div class="meta">
            Size: <strong>{size_mb:.1f} MB</strong> &bull; Package: <strong>com.swailumzafar.nativegallery</strong>
        </div>

        <div class="direct-link-box">
            <a href="/{PUBLIC_FILENAME}">{PUBLIC_FILENAME}</a>
            <button class="copy-btn" onclick="copyLink()">Copy URL</button>
        </div>

        {f'''
        <div class="qr-section">
            <div class="qr-label">📱 Scan with Phone Camera to Install</div>
            <div class="qr-container">
                {qr_svg}
            </div>
        </div>''' if qr_svg else ''}

        <div class="features">
            <strong class="title">✨ What's Updated in v{VERSION}:</strong>
            <ul>
                <li>🌊 <strong>ColorOS 17 Living Flow Pinch</strong>: Pinch in/out scales pictures and videos content inside the grid, while header and search stay rock-solid.</li>
                <li>📁 <strong>Ultra-Smooth Folder Transitions</strong>: Zero-lag opening and closing on folders with videos via GPU layer transforms.</li>
                <li>🎬 <strong>Lag-Free Video Dismiss</strong>: Seamless 1:1 pull-down dismiss with paused playback and asynchronous release overhead.</li>
                <li>⚡ <strong>Glitch-Free Folder Close</strong>: Interruptible closing animation with zero dropped taps and no folder refresh ripples.</li>
            </ul>
        </div>

        <div class="instructions">
            <strong>Installation Steps:</strong>
            <ol>
                <li>Tap <strong>Download &amp; Install APK</strong> above (or scan the QR code).</li>
                <li>If prompted by Chrome or your browser, tap <strong>Download anyway</strong>.</li>
                <li>When the file finishes downloading, tap <strong>Open</strong>.</li>
                <li>If prompted, allow installation from unknown sources for your browser, then tap <strong>Install / Update</strong>.</li>
            </ol>
        </div>
    </div>

    <script>
        function copyLink() {{
            const link = window.location.origin + "/{PUBLIC_FILENAME}";
            navigator.clipboard.writeText(link).then(() => {{
                const btn = document.querySelector('.copy-btn');
                btn.textContent = 'Copied!';
                setTimeout(() => {{ btn.textContent = 'Copy URL'; }}, 2000);
            }}).catch(() => {{
                prompt('Copy this download link:', link);
            }});
        }}
    </script>
</body>
</html>"""
        data = html.encode("utf-8")
        try:
            self.send_response(200)
            self.send_cors_headers()
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
            self.send_header("Connection", "close")
            self.close_connection = True
            self.end_headers()
            self.wfile.write(data)
            self.wfile.flush()
        except (ConnectionResetError, BrokenPipeError, ConnectionAbortedError, OSError):
            pass

    def handle_one_request(self):
        try:
            super().handle_one_request()
        except (ConnectionResetError, ConnectionAbortedError, BrokenPipeError, TimeoutError, OSError):
            self.close_connection = True

    def serve_apk_file(self, path, total_size):
        range_header = self.headers.get("Range")
        start = 0
        end = total_size - 1

        if range_header:
            match = re.match(r"bytes=(\d+)-(\d*)", range_header)
            if match:
                start = int(match.group(1))
                if match.group(2):
                    end = int(match.group(2))
                end = min(end, total_size - 1)
            else:
                self.send_error(416, "Requested Range Not Satisfiable")
                return

        if start > end or start >= total_size:
            self.send_response(416)
            self.send_header("Content-Range", f"bytes */{total_size}")
            self.end_headers()
            return

        length = end - start + 1

        try:
            if range_header:
                self.send_response(206)
                self.send_header("Content-Range", f"bytes {start}-{end}/{total_size}")
            else:
                self.send_response(200)

            self.send_cors_headers()
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", f'attachment; filename="{PUBLIC_FILENAME}"')
            self.send_header("Content-Length", str(length))
            self.send_header("Accept-Ranges", "bytes")
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
            self.send_header("Connection", "close")
            self.close_connection = True
            self.end_headers()

            with open(path, "rb") as f:
                f.seek(start)
                remaining = length
                chunk_size = 64 * 1024
                sent = 0
                while remaining > 0:
                    read_len = min(remaining, chunk_size)
                    chunk = f.read(read_len)
                    if not chunk:
                        break
                    self.wfile.write(chunk)
                    self.wfile.flush()
                    sent += len(chunk)
                    remaining -= len(chunk)
            print(f"Successfully served {sent} bytes of APK to {self.client_address[0]}", flush=True)
        except (ConnectionResetError, BrokenPipeError, ConnectionAbortedError, TimeoutError, OSError) as e:
            print(f"Client disconnected during download ({e}): {self.client_address[0]}", flush=True)

class ThreadedServer(http.server.ThreadingHTTPServer):
    allow_reuse_address = True
    daemon_threads = True

def print_server_info(ports):
    primary_ip = get_primary_ip()
    port_primary = ports[0]
    print("=" * 64, flush=True)
    print(f"  NATIVE GALLERY v{VERSION} - INSTALLATION SERVER READY", flush=True)
    print("=" * 64, flush=True)
    for p in ports:
        print(f"  Web Page (Port {p}):   http://{primary_ip}:{p}/", flush=True)
        print(f"  Direct APK (Port {p}):  http://{primary_ip}:{p}/{PUBLIC_FILENAME}", flush=True)
    print("-" * 64, flush=True)
    print(f"  Local Machine:        http://localhost:{port_primary}/", flush=True)
    print("=" * 64, flush=True)

    if HAS_QRCODE:
        try:
            print("\n  SCAN WITH PHONE CAMERA TO INSTALL:\n", flush=True)
            qr = qrcode.QRCode(border=1)
            qr.add_data(f"http://{primary_ip}:{port_primary}/")
            qr.make(fit=True)
            try:
                qr.print_ascii(invert=True)
            except Exception:
                pass
            try:
                from PIL import Image
                img = qr.make_image(fill_color="black", back_color="white")
                img.save("wifi_install_qr.png")
            except Exception:
                pass
            print("\n" + "=" * 64, flush=True)
        except Exception:
            pass

def start_server_on_port(port):
    while True:
        try:
            httpd = ThreadedServer(("0.0.0.0", port), ApkServerHandler)
            print(f"Server listening on port {port}...", flush=True)
            httpd.serve_forever()
        except KeyboardInterrupt:
            break
        except Exception as e:
            print(f"Server on port {port} stopped: {e}", flush=True)
            time.sleep(1)

if __name__ == "__main__":
    import threading
    cleanup_old_instances()
    PORTS = [8080, 8000]
    print_server_info(PORTS)
    
    for port in PORTS[1:]:
        t = threading.Thread(target=start_server_on_port, args=(port,), daemon=True)
        t.start()
    
    start_server_on_port(PORTS[0])
