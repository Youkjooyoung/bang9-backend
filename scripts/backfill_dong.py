#!/usr/bin/env python3
"""
LISTINGS 테이블의 DONG_NAME / DONG_CODE / GU_NAME / GU_CODE를
좌표(LATITUDE, LONGITUDE) 기준으로 카카오 행정동 정보로 백필한다.

사전 조건:
- KAKAO_REST_API_KEY 환경변수 또는 .env 파일에 설정되어 있어야 함
- mysql client가 PATH에 있고 docker exec bang9-mysql 사용 가능해야 함
"""
import json
import os
import subprocess
import sys
import time
import urllib.parse
import urllib.request

KAKAO_URL = "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json"


def load_env_key() -> str:
    key = os.environ.get("KAKAO_REST_API_KEY")
    if key:
        return key
    env_path = os.path.join(os.path.dirname(__file__), "..", "..", ".env")
    env_path = os.path.normpath(env_path)
    if os.path.isfile(env_path):
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line.startswith("KAKAO_REST_API_KEY="):
                    return line.split("=", 1)[1].strip().strip('"').strip("'")
    raise RuntimeError("KAKAO_REST_API_KEY를 찾을 수 없습니다.")


def fetch_region(lat: float, lng: float, key: str) -> dict | None:
    qs = urllib.parse.urlencode({"x": lng, "y": lat})
    req = urllib.request.Request(
        f"{KAKAO_URL}?{qs}",
        headers={"Authorization": f"KakaoAK {key}"},
    )
    with urllib.request.urlopen(req, timeout=10) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    docs = data.get("documents") or []
    h = next((d for d in docs if d.get("region_type") == "H"), None)
    return h or (docs[0] if docs else None)


def docker_mysql(query: str) -> str:
    cmd = [
        "docker", "exec", "bang9-mysql",
        "mysql", "--default-character-set=utf8mb4",
        "-ubang9", "-pbang9", "-N", "-B", "bang9", "-e", query,
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8")
    if proc.returncode != 0:
        raise RuntimeError(f"mysql 실행 실패: {proc.stderr}")
    return proc.stdout


def main() -> int:
    key = load_env_key()
    rows_raw = docker_mysql(
        "SELECT LISTING_ID, LATITUDE, LONGITUDE FROM LISTINGS "
        "WHERE STATUS='ACTIVE' AND DELETED_AT IS NULL "
        "AND LATITUDE IS NOT NULL AND LONGITUDE IS NOT NULL"
    )
    rows = []
    for line in rows_raw.strip().splitlines():
        parts = line.split("\t")
        if len(parts) != 3:
            continue
        rows.append((int(parts[0]), float(parts[1]), float(parts[2])))
    print(f"대상 매물: {len(rows)}건")

    success = 0
    failed = 0
    update_stmts = []
    for idx, (listing_id, lat, lng) in enumerate(rows, start=1):
        try:
            region = fetch_region(lat, lng, key)
            if region is None:
                failed += 1
                continue
            gu_name = (region.get("region_2depth_name") or "").strip()
            dong_name = (region.get("region_3depth_name") or "").strip()
            gu_code = (region.get("code") or "")[:5]
            dong_code = region.get("code") or ""
            if not gu_name or not dong_name:
                failed += 1
                continue
            gu_name_e = gu_name.replace("'", "''")
            dong_name_e = dong_name.replace("'", "''")
            update_stmts.append(
                f"UPDATE LISTINGS SET GU_CODE='{gu_code}', GU_NAME='{gu_name_e}', "
                f"DONG_CODE='{dong_code}', DONG_NAME='{dong_name_e}' "
                f"WHERE LISTING_ID={listing_id};"
            )
            success += 1
        except Exception as e:
            print(f"  실패 listing_id={listing_id}: {e}", file=sys.stderr)
            failed += 1
        if idx % 20 == 0:
            print(f"  진행 {idx}/{len(rows)} (성공 {success}, 실패 {failed})")
        time.sleep(0.05)

    print(f"\n조회 완료: 성공 {success}, 실패 {failed}")
    if not update_stmts:
        print("업데이트할 항목 없음.")
        return 1

    sql = "\n".join(update_stmts)
    print(f"\nUPDATE 실행 중... ({len(update_stmts)}건)")
    docker_mysql(sql)
    print("완료.")

    print("\n분포 확인:")
    print(docker_mysql(
        "SELECT GU_NAME, DONG_NAME, COUNT(*) AS cnt FROM LISTINGS "
        "WHERE STATUS='ACTIVE' AND DELETED_AT IS NULL "
        "GROUP BY GU_NAME, DONG_NAME ORDER BY cnt DESC"
    ))
    return 0


if __name__ == "__main__":
    sys.exit(main())
