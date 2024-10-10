import time
from flask import Flask, Response, request, jsonify
from flask_cors import CORS, cross_origin
import pylint.lint
import pylint.reporters
import tempfile
import os

app = Flask(__name__)
CORS(app)
app.config["DEBUG"] = True
app.config["CORS_HEADERS"] = "Content-Type"


@app.route("/")
@cross_origin(
    origins="*",
    methods=["GET"],
)
def hello_world():
    return jsonify({"message": "Hello, World!"}), 200


@app.route("/api/analyze", methods=["POST"])
@cross_origin(
    origins="*",
    methods=["POST"],
)
def analyze_code():
    code = request.get_json().get("code")
    try:
        tempname = ""
        with tempfile.NamedTemporaryFile(suffix=".py", dir="./", delete=False) as temp:
            temp.write(code.encode("utf-8"))
            tempname = temp.name.split("\\")[-1]
        reporter = pylint.reporters.CollectingReporter()
        pylint_output = pylint.lint.Run([tempname], exit=False, reporter=reporter)
        os.remove(tempname)
        return (
            jsonify(
                {
                    "results": [{"line":message.line , "description": message.msg} for message in reporter.messages],
                    "message": "Code analyzed successfully",
                }
            ),
            200,
        )

    except Exception as e:
        return jsonify({"message": str(e)}), 500
