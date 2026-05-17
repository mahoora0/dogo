import logging
import logging.handlers
import os
from pathlib import Path

LOG_DIR = Path(os.getenv("LOG_DIR", "logs"))
LOG_DIR.mkdir(exist_ok=True)

_CONSOLE_FMT = logging.Formatter(
    "[%(asctime)s] %(levelname)-8s | %(message)s",
    datefmt="%H:%M:%S",
)
_FILE_FMT = logging.Formatter(
    "%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)


def get_logger(name: str) -> logging.Logger:
    logger = logging.getLogger(name)
    if logger.handlers:
        return logger

    logger.setLevel(logging.DEBUG)

    console = logging.StreamHandler()
    console.setLevel(logging.INFO)
    console.setFormatter(_CONSOLE_FMT)

    file_handler = logging.handlers.RotatingFileHandler(
        LOG_DIR / "match.log",
        maxBytes=5 * 1024 * 1024,
        backupCount=3,
        encoding="utf-8",
    )
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(_FILE_FMT)

    logger.addHandler(console)
    logger.addHandler(file_handler)
    return logger
