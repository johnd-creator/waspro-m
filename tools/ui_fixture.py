"""Loopback-only, synthetic UI fixture. No connection to WASPRO or operational data.

Run: python tools/ui_fixture.py
Use developmentApiUrl=http://127.0.0.1:8798/api/v1/ with adb reverse tcp:8798 tcp:8798.
This validates rendering/navigation, not backend behavior or authorization enforcement.
All mutations except fake login/logout return 405. DEMO-ERROR search / year=2099
simulates 503; nonempty search / year=2000 returns empty data.
DEMO-SLOW search on waste-records delays an empty response by 2 seconds.
"""
import json
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

CONTEXT = {
    "user": {"id": "ui-demo", "name": "Aditya", "email": "aditya@example.test"},
    "organization": {"id": "ui-demo", "name": "Unit Nusantara · DEMO", "code": "DEMO"},
    "role": {"name": "Supervisor"},
    "permissions": ["waste_records.view_all", "waste_records.create", "waste_records.approve",
        "waste_hauling.view_all", "faba_dashboard.view", "faba_production.view",
        "faba_production.create", "faba_utilization.view", "faba_adjustments.view",
        "faba_approvals.view", "faba_recaps.view"],
}
ROWS = [
    {"id": str(index), "record_number": f"DEMO-LMB/2026/00{index}", "date": "2026-11-04",
     "waste_type": {"id": str(index), "name": name}, "quantity": quantity, "unit": "kg",
     "source": source, "status": status, "resource_version": "demo-1",
     "created_by": {"name": "Operator Demo"}, "allowed_actions": ["view"]}
    for index, name, quantity, source, status in [
        (1, "Oli pelumas bekas", "1250.50", "Area turbin · Unit 1", "pending_review"),
        (2, "Filter oli bekas", "48", "Workshop pemeliharaan", "approved"),
        (3, "Kemasan terkontaminasi", "86.25", "Gudang material", "draft"),
    ]
]
DASHBOARD = {"organization": CONTEXT["organization"],
    "waste": {"total_records": 128, "pending_records": 8, "draft_records": 12, "approved_records": 108},
    "faba": {"current_balance": "2480.75", "total_production": "3260.50", "total_utilization": "779.75"}}


# Synthetic permissions expose layouts only; every real mutation is rejected below.
ROWS[0]["allowed_actions"] = ["view", "approve", "reject"]
CONTEXT["role"]["name"] = "Supervisor DEMO"
CONTEXT["permissions"] += ["faba_opening_balance.manage", "faba_reports.export", "users.view", "roles.view",
    "waste_hauling.create", "waste_hauling.approve", "faba_approvals.approve"] + [
    f"{module}.{action}" for module in ("faba_production", "faba_utilization", "faba_adjustments",
    "waste_categories", "waste_characteristics", "waste_types", "vendors") for action in ("view", "create")]
MASTER = {key: [{"id": f"demo-{key}", "name": f"DEMO {name}", "code": "DEMO",
    "description": "Data sintetis", "is_active": True, "allowed_actions": ["view"]}]
    for key, name in (("categories", "Kategori"), ("characteristics", "Karakteristik"), ("types", "Jenis"), ("vendors", "Vendor"))}
LOOKUPS = {"waste_types": [r["waste_type"] for r in ROWS], "waste_categories": MASTER["categories"],
    "waste_characteristics": MASTER["characteristics"], "vendors": MASTER["vendors"],
    "internal_destinations": [{"id": "demo-internal", "name": "DEMO Area internal"}],
    "purposes": [{"id": "demo-purpose", "name": "DEMO Pemeliharaan"}]}
OPTIONS = {**LOOKUPS, "materials": ["fly_ash", "bottom_ash"], "production_movement_types": ["production"],
    "utilization_movement_types": ["utilization_internal", "utilization_external"],
    "adjustment_movement_types": ["adjustment_in", "adjustment_out"]}
MOVEMENTS = {key: [{"id": f"demo-{key}", "display_number": f"DEMO-{key.upper()}-001",
    "transaction_date": "2026-11-04", "material_type": "fly_ash", "movement_type": movement,
    "quantity": "125.50", "unit": "ton", "approval_status": "pending_review",
    "note": "DEMO - bukan data operasional", "allowed_actions": ["view"], "resource_version": "demo-1"}]
    for key, movement in (("production", "production"), ("utilization", "utilization_internal"), ("adjustments", "adjustment_in"))}
PERIOD = {"id": "demo-period", "year": 2026, "month": 11, "period_label": "DEMO November 2026",
    "status": "pending_review", "allowed_actions": ["view"]}
HAULING = {"id": "demo-hauling", "hauling_number": "DEMO-ANGKUT-001", "hauling_date": "2026-11-04",
    "quantity": "25", "unit": "kg", "status": "approved", "waste_record": ROWS[0], "allowed_actions": ["view"]}
BACKLOG = {**ROWS[0], "remaining_quantity": "1225.50", "reserved_quantity": "25", "allowed_actions": ["view", "create_hauling"]}
RECAP = {"period_label": "DEMO November 2026", "material_type": "fly_ash", "opening_balance": "0",
    "total_production": "3260.50", "total_utilization": "779.75", "closing_balance": "2480.75"}
EXTRA_ROUTES = {"waste-haulings": [BACKLOG], "waste-haulings/pending-approval": [HAULING],
    "waste-haulings/history/1": {"record": BACKLOG, "haulings": [HAULING]}, "waste-haulings/demo-hauling": HAULING,
    "faba/approvals": [PERIOD], "faba/approvals/2026/11": {"approval": PERIOD, "recap": RECAP},
    "faba/approvals/history": [{"name": "DEMO Riwayat persetujuan", "status": "approved"}],
    "faba/movements/pending-approval": sum(MOVEMENTS.values(), []),
    "users": [{"id": "demo-user", "name": "DEMO Operator", "email": "operator@example.test", "allowed_actions": ["view"]}],
    "roles": [{"id": "demo-role", "name": "DEMO Supervisor", "allowed_actions": ["view"]}]}
for key, rows in MOVEMENTS.items():
    EXTRA_ROUTES[f"faba/{key}"] = rows
    EXTRA_ROUTES[f"faba/{key}/{rows[0]['id']}"] = rows[0]
for key, rows in MASTER.items():
    EXTRA_ROUTES[f"master-data/{key}"] = rows
    EXTRA_ROUTES[f"master-data/{key}/{rows[0]['id']}"] = rows[0]
for key in ("monthly", "yearly", "vendors", "balance", "stock-card", "opening-balance", "tps-capacity"):
    EXTRA_ROUTES[f"faba/recaps/{key}"] = RECAP
for key in ("users", "roles"):
    EXTRA_ROUTES[f"{key}/{EXTRA_ROUTES[key][0]['id']}"] = EXTRA_ROUTES[key][0]


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *_):
        pass  # Never log request bodies or credentials.

    def reply(self, data, status=200, message=""):
        body = json.dumps({"success": status < 400, "data": data, "meta": {}, "message": message}, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Connection", "close")
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)
        self.close_connection = True

    def reject_write(self):
        self.reply(None, 405, "DEMO: perubahan data dinonaktifkan.")

    do_PUT = do_PATCH = do_DELETE = reject_write

    def do_POST(self):
        # Do not read, store, or log submitted credentials or mutation bodies.
        if self.path == "/api/v1/auth/login":
            # A placeholder session accepted by this process only; never a real credential.
            self.reply({"token": "synthetic-ui-fixture", "expires_at": "2099-01-01T00:00:00Z", "context": CONTEXT})
        elif self.path == "/api/v1/auth/logout":
            self.reply(None)
        else:
            self.reject_write()

    def do_GET(self):
        url = urlparse(self.path)
        path = url.path.removeprefix("/api/v1/")
        query = parse_qs(url.query)
        if not url.path.startswith("/api/v1/"):
            return self.reply(None, 404)
        # Read-only deterministic UI states. No administration/mutation endpoint.
        if path == "waste-records" and query.get("search") == ["DEMO-SLOW"]:
            time.sleep(2)
            return self.reply([])
        if query.get("search") == ["DEMO-ERROR"] or query.get("year") == ["2099"]:
            return self.reply(None, 503, "DEMO: simulasi layanan tidak tersedia.")
        if query.get("year") == ["2000"]:
            return self.reply([])
        if path in EXTRA_ROUTES:
            return self.reply(EXTRA_ROUTES[path])
        if path == "me":
            self.reply(CONTEXT)
        elif path == "dashboard":
            self.reply(DASHBOARD)
        elif path == "profile":
            self.reply(CONTEXT["user"])
        elif path == "lookups":
            self.reply(LOOKUPS)
        elif path == "faba/options":
            self.reply(OPTIONS)
        elif path == "faba/dashboard":
            self.reply({"current_balance": "2480.75", "resolved_period": {"month": 11, "year": 2026},
                        "recap": {"total_production": "3260.50", "total_utilization": "779.75"}})
        elif path in ("waste-records", "waste-records/pending-approval"):
            rows = ROWS if path == "waste-records" else ROWS[:1]
            self.reply([] if parse_qs(url.query).get("search") else rows)
        elif path.startswith("waste-records/"):
            self.reply(next((r for r in ROWS if r["id"] == path.split("/")[-1]), {}))
        else:
            self.reply(None, 404)


if __name__ == "__main__":
    print("Synthetic UI fixture listening on 127.0.0.1:8798", flush=True)
    ThreadingHTTPServer(("127.0.0.1", 8798), Handler).serve_forever()
