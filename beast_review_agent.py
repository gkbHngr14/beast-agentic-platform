from langgraph.graph import StateGraph, END
from typing import TypedDict
from langchain_ollama import OllamaLLM

# Initialize once at top of file
llm = OllamaLLM(model="llama3.2:3b")

# State
class ReviewAgentState(TypedDict):
    metrics: dict
    rag_context: str
    risk_score: float
    recommendation: str

# Node 1: ingest metrics
def ingest_metrics(state: ReviewAgentState):
    # Use metrics from state if provided, 
    # otherwise use defaults
    metrics = state["metrics"] if state["metrics"] else {
        "latency" : 450,
        "error_rate": 0.02,
        "throughput": 8300
    }

    print(f"metrics ingested: {metrics}")
    return {"metrics": metrics}

# second node — retrieve_context — 
#which takes the metrics from state and calls your RAG service to get relevant context.

# Node 2: retrieve context
def retrieve_context(state: ReviewAgentState):
    #Once fully wired up, this has to call the Beast's RAGService
    #defaulting for now
    rag_context = state["rag_context"] if state["rag_context"] else "fraud_patterns: deepfake attempt"  

    print(f"rag_context retrieved: {rag_context}")
    return {"rag_context": rag_context}

# Node 3: reason - this takes metrics and the rag context to generate reason via LLM call
def reason(state: ReviewAgentState):
    prompt = f"""
You are a fraud risk analyst. Classify the risk level as HIGH, MEDIUM, or LOW.

Examples:
- Metrics: latency=900, error_rate=0.15, throughput=100, Context: deepfake attempt → HIGH
- Metrics: latency=200, error_rate=0.01, throughput=9000, Context: standard transaction → LOW
- Metrics: latency=600, error_rate=0.05, throughput=4000, Context: velocity abuse → MEDIUM

Now classify:
Metrics: {state['metrics']}
Context: {state['rag_context']}

Respond with ONLY one word: HIGH, MEDIUM, or LOW
"""
    response = llm.invoke(prompt)
    print(f"Reasoning: {response}")
    return {"recommendation": response}
    
# Router function - decides next node based on recommendation
def route_by_risk(state: ReviewAgentState):
    recommendation = state["recommendation"].strip().upper()
    first_word = recommendation.split()[0]  # just the first word
    print(f"DEBUG - first_word: '{first_word}'")  # add this
    if first_word == "HIGH":
        return "escalate"
    if first_word == "MEDIUM":
        return "file_case"
    return "summarize"

# Node 4: escalate
def escalate(state: ReviewAgentState):
    print(f"HIGH RISK — escalating to human review")
    return {"recommendation": state["recommendation"] + "\n[ESCALATED TO HUMAN REVIEW]"}

# Node 5: summarize
def summarize(state: ReviewAgentState):
    print(f"Risk within acceptable range — summarizing")
    return {"recommendation": state["recommendation"] + "\n[AUTO-APPROVED]"}

# Node 6: file case
def file_case(state: ReviewAgentState):
    print(f"Risk is medium — filing case and logging")
    return {"recommendation": state["recommendation"] + "\n[CASE-FILED]"}

# Build the graph actually now out of the agent state..!
graph = StateGraph(ReviewAgentState)

# Add nodes
graph.add_node("ingest_metrics", ingest_metrics)
graph.add_node("retrieve_context", retrieve_context)
graph.add_node("reason", reason)

# Add edges
graph.set_entry_point("ingest_metrics")
graph.add_edge("ingest_metrics", "retrieve_context")
graph.add_edge("retrieve_context", "reason")

#graph.add_edge("reason", END)

graph.add_node("escalate", escalate)
graph.add_node("summarize", summarize)
graph.add_node("file_case", file_case)

graph.add_edge("escalate", END)
graph.add_edge("summarize", END)
graph.add_edge("file_case", END)

graph.add_conditional_edges(
    "reason",
    route_by_risk,
    {
        "escalate": "escalate",
        "file_case": "file_case",
        "summarize": "summarize"
    }
)

# Compile
app = graph.compile()

# Run

if __name__ == "__main__":
    result = app.invoke({
      "metrics": {},
      "rag_context": "",
      "risk_score": 0.0,
      "recommendation": ""
    })
    print(f"Final result: {result}")