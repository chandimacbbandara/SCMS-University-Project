#!/usr/bin/env python3
import argparse
import os
import pickle
import re
import sys
import warnings
from typing import Any

import joblib

PRIORITY_HIGH = "High"
PRIORITY_MEDIUM = "Medium"
PRIORITY_LOW = "Low"
DEFAULT_BASE_MODEL = "distilbert-base-uncased"


def normalize_label(value: Any) -> str:
    if value is None:
        return PRIORITY_MEDIUM

    text = str(value).strip().lower()
    if text.startswith("high"):
        return PRIORITY_HIGH
    if text.startswith("low"):
        return PRIORITY_LOW
    if text.startswith("med"):
        return PRIORITY_MEDIUM
    return PRIORITY_MEDIUM


def normalize_text(value: str) -> str:
    if value is None:
        return ""
    value = re.sub(r"\s+", " ", value).strip()
    return value[:1500]


def fallback_priority(value: Any) -> str:
    return normalize_label(value)


def load_label_encoder(path: str) -> Any:
    if not os.path.exists(path):
        return None

    try:
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            return joblib.load(path)
    except Exception:
        try:
            with open(path, "rb") as handle:
                return pickle.load(handle)
        except Exception:
            labels = extract_labels_from_pickle(path)
            return labels if labels else None


def extract_labels_from_pickle(path: str) -> list[str]:
    try:
        raw = open(path, "rb").read().decode("latin1", errors="ignore")
    except Exception:
        return []

    matches = re.findall(r"(HIGH|LOW|MEDIUM|High|Low|Medium|high|low|medium)", raw)
    labels: list[str] = []
    for item in matches:
        normalized = item.upper()
        if normalized not in labels:
            labels.append(normalized)

    return labels


def decode_label(predicted_index: int, label_encoder: Any) -> str:
    if isinstance(label_encoder, (list, tuple)):
        if 0 <= predicted_index < len(label_encoder):
            return normalize_label(label_encoder[predicted_index])
        return PRIORITY_MEDIUM

    if label_encoder is None:
        return normalize_label(predicted_index)

    try:
        if hasattr(label_encoder, "inverse_transform"):
            value = label_encoder.inverse_transform([predicted_index])[0]
            return normalize_label(value)
    except Exception:
        pass

    try:
        classes = getattr(label_encoder, "classes_", None)
        if classes is not None and len(classes) > predicted_index:
            return normalize_label(classes[predicted_index])
    except Exception:
        pass

    return PRIORITY_MEDIUM


def normalize_state_dict_keys(state_dict: dict[str, Any]) -> dict[str, Any]:
    normalized: dict[str, Any] = {}
    for key, value in state_dict.items():
        mapped = key
        if mapped.endswith(".beta"):
            mapped = mapped[:-5] + ".bias"
        elif mapped.endswith(".gamma"):
            mapped = mapped[:-6] + ".weight"
        normalized[mapped] = value
    return normalized


def model_priority(model_dir: str, label_encoder: Any, category: str, subject: str, message: str) -> str:
    model_path = os.path.join(model_dir, "model.safetensors")
    if not os.path.exists(model_path):
        raise RuntimeError("model.safetensors not found")

    import torch
    from safetensors.torch import load_file as load_safetensors
    from transformers import AutoTokenizer, DistilBertConfig, DistilBertForSequenceClassification

    state_dict = normalize_state_dict_keys(load_safetensors(model_path))
    if "classifier.weight" not in state_dict:
        raise RuntimeError("classifier.weight is missing from model.safetensors")

    num_labels = int(state_dict["classifier.weight"].shape[0])
    model = DistilBertForSequenceClassification(DistilBertConfig(num_labels=num_labels))
    missing_keys, unexpected_keys = model.load_state_dict(state_dict, strict=False)
    if unexpected_keys:
        raise RuntimeError(f"Unexpected model keys: {unexpected_keys[:5]}")
    if missing_keys:
        raise RuntimeError(f"Missing model keys: {missing_keys[:5]}")

    tokenizer = None
    local_tokenizer_files = [
        os.path.join(model_dir, "tokenizer.json"),
        os.path.join(model_dir, "tokenizer_config.json"),
        os.path.join(model_dir, "vocab.txt")
    ]

    if any(os.path.exists(path) for path in local_tokenizer_files):
        tokenizer = AutoTokenizer.from_pretrained(model_dir, local_files_only=True)
    else:
        # Use the default tokenizer for DistilBERT when only weights are available.
        try:
            tokenizer = AutoTokenizer.from_pretrained(DEFAULT_BASE_MODEL, local_files_only=True)
        except Exception:
            tokenizer = AutoTokenizer.from_pretrained(DEFAULT_BASE_MODEL)

    model.eval()

    # Use plain training-style text instead of tagged prompt format.
    # Most concern classifiers are trained on raw concern text (message/subject),
    # and tag prefixes can skew predictions toward a single class.
    combined_text = message.strip()
    if not combined_text:
        combined_text = f"{subject} {category}".strip()
    elif subject.strip():
        combined_text = f"{subject.strip()} {combined_text}".strip()

    encoded = tokenizer(
        combined_text,
        return_tensors="pt",
        truncation=True,
        padding=True,
        max_length=256
    )

    with torch.no_grad():
        logits = model(**encoded).logits

    predicted_index = int(logits.argmax(dim=1).item())
    return decode_label(predicted_index, label_encoder)


def main() -> int:
    parser = argparse.ArgumentParser(description="Predict concern priority")
    parser.add_argument("--model-dir", required=True)
    parser.add_argument("--category", default="")
    parser.add_argument("--subject", default="")
    parser.add_argument("--message", default="")
    parser.add_argument("--fallback-priority", default=PRIORITY_MEDIUM)
    args = parser.parse_args()

    category = normalize_text(args.category)
    subject = normalize_text(args.subject)
    message = normalize_text(args.message)

    encoder_path = os.path.join(args.model_dir, "label_encoder.pkl")
    label_encoder = load_label_encoder(encoder_path)

    try:
        result = model_priority(args.model_dir, label_encoder, category, subject, message)
        print(normalize_label(result))
        return 0
    except Exception:
        # Keep concern submission flow stable when model artifacts/runtime are not ready.
        print(fallback_priority(args.fallback_priority))
        return 0


if __name__ == "__main__":
    sys.exit(main())
