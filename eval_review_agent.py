from beast_review_agent import app

test_cases = [
    {
        "metrics": {"latency": 900, "error_rate": 0.15, "throughput": 100},
        "rag_context": "fraud_patterns: deepfake attempt",
        "expected": "HIGH"
    },
    {
        "metrics": {"latency": 450, "error_rate": 0.02, "throughput": 8300},
        "rag_context": "normal_patterns: standard transaction",
        "expected": "LOW"
    },
    {
        "metrics": {"latency": 600, "error_rate": 0.05, "throughput": 4000},
        "rag_context": "fraud_patterns: velocity abuse",
        "expected": "MEDIUM"
    },
]

# Eval harness
def run_eval():
    correct = 0
    total = len(test_cases)

    for i, test in enumerate(test_cases):
        result = app.invoke({
            "metrics": test["metrics"],
            "rag_context": test["rag_context"],
            "risk_score": 0.0,
            "recommendation": ""
            })

        actual = result["recommendation"].strip().split()[0].upper()
        expected = test["expected"]
        passed = actual == expected

        if passed:
            correct += 1

        print(f"Test {i+1}: expected={expected}, actual={actual}, {'PASS' if passed else 'FAIL'}")

    print(f"\nAccuracy: {correct}/{total} = {correct/total*100:.1f}%")

run_eval()

    