#!/usr/bin/env python3
"""Small Gradle bootstrap, NOT the official Gradle Wrapper. Requires Python 3."""
import hashlib, os, pathlib, shutil, subprocess, sys, tempfile, urllib.request, zipfile
VERSION = "8.11.1"
ROOT = pathlib.Path(__file__).resolve().parents[1]
CACHE = pathlib.Path(os.environ.get("GRADLE_USER_HOME", pathlib.Path.home()/".gradle"))/"kotoba-bootstrap"
HOME = CACHE / ("gradle-"+VERSION)
URL = "https://services.gradle.org/distributions/gradle-"+VERSION+"-bin.zip"
def download(url, path):
    request = urllib.request.Request(url, headers={"User-Agent":"KotobaWall-GradleBootstrap/1.0"})
    with urllib.request.urlopen(request, timeout=120) as r, open(path,"wb") as f:
        shutil.copyfileobj(r,f)
def main():
    executable = HOME / "bin" / ("gradle.bat" if os.name=="nt" else "gradle")
    if not executable.exists():
        CACHE.mkdir(parents=True,exist_ok=True)
        print("Downloading Gradle",VERSION,"from services.gradle.org. First build needs internet.",flush=True)
        with tempfile.TemporaryDirectory(dir=CACHE) as temp:
            temp=pathlib.Path(temp); archive=temp/"gradle.zip"; checksum=temp/"checksum.txt"
            download(URL+".sha256",checksum)
            expected=checksum.read_text().strip().split()[0].lower()
            if len(expected)!=64 or any(c not in "0123456789abcdef" for c in expected):
                raise RuntimeError("Invalid Gradle checksum response")
            download(URL,archive)
            with archive.open("rb") as f:
                digest=hashlib.file_digest(f,"sha256").hexdigest() if hasattr(hashlib,"file_digest") else hashlib.sha256(f.read()).hexdigest()
            if digest!=expected: raise RuntimeError("Gradle checksum verification failed")
            with zipfile.ZipFile(archive) as z:
                for member in z.infolist():
                    target=(temp/member.filename).resolve()
                    if temp.resolve() not in target.parents:
                        raise RuntimeError("Unsafe ZIP entry")
                z.extractall(temp)
            shutil.move(str(temp/("gradle-"+VERSION)),str(HOME))
        if os.name!="nt": executable.chmod(0o755)
    return subprocess.call([str(executable),*sys.argv[1:]],cwd=ROOT)
if __name__=="__main__":
    try: sys.exit(main())
    except Exception as e:
        print("Build setup failed:",e,file=sys.stderr)
        print("See README.md for Java 17, Android SDK and internet setup.",file=sys.stderr)
        sys.exit(1)
