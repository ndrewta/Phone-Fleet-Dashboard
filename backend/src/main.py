from contextlib import asynccontextmanager
from datetime import datetime
from dotenv import load_dotenv
import os
from pydantic import BaseModel
from tapo import ApiClient
from fastapi import FastAPI
import json
import uvicorn


BASE_DIR = os.path.dirname(os.path.abspath(__file__))
docker_mode = False

# 1. Define the Paths
# Docker Path (Sibling to src)
env_docker = os.path.join(BASE_DIR, "..", ".env")
config_docker = os.path.join(BASE_DIR, "..", "config", "devices.json")

# Local Path (Root folder - two steps back)
env_root = os.path.join(BASE_DIR, "..", "..", ".env")
config_root = os.path.join(BASE_DIR, "..", "..", "config", "devices.json")

# 2. The Logic: Find the Config first, then load the matching .env
if os.path.exists(config_docker):
    load_dotenv(env_docker)
    CONFIG_PATH = config_docker
    docker_mode = True
    print("SYSTEM: Running in Docker mode")
else:
    load_dotenv(env_root)
    CONFIG_PATH = config_root
    print("SYSTEM: Running in Local mode")


class DeviceReport(BaseModel):
    device_id: str
    battery_level: int
    is_charging: bool
    plug_slot: int


def load_config():
    if os.path.exists(CONFIG_PATH):
        with open(CONFIG_PATH, "r") as f:
            return json.load(f)
    return {}


def save_config(config):
    with open(CONFIG_PATH, "w") as f:
        json.dump(config, f, indent=4)


@asynccontextmanager
async def lifespan(app: FastAPI):
    print("Starting up...")

    client = ApiClient(os.getenv("TAPO_USERNAME"), os.getenv("TAPO_PASSWORD"))
    app.state.power_board = await client.p300(os.getenv("TAPO_P300_IP"))
    
    yield
    print("Shutting down...")

app = FastAPI(lifespan=lifespan)


@app.post("/devices/{device_id}")
async def handle_device_report(device: DeviceReport):

    device_list = load_config()
    timestamp = datetime.now().strftime("%H:%M:%S : %d-%m-%Y")

    if device.device_id not in device_list:
        device_list[device.device_id] =  {}
        print(f"New device {device.device_id} added to config.")

    if device.battery_level <= int(os.getenv("BATTERY_LOW_THRESHOLD")) and not device.is_charging:
        plug = await app.state.power_board.plug(position=device.plug_slot)
        await plug.on()
        print(f"Device {device.device_id} is low on battery. Plug {device.plug_slot} turned ON.")
    elif device.battery_level >= int(os.getenv("BATTERY_HIGH_THRESHOLD")) and device.is_charging:
        plug = await app.state.power_board.plug(position=device.plug_slot)
        await plug.off()
        print(f"Device {device.device_id} is fully charged. Plug {device.plug_slot} turned OFF.")

    updated_device_data = device.model_dump()
    updated_device_data["last_updated"] = timestamp

    device_list[device.device_id].update(updated_device_data)
    save_config(device_list)


if __name__ == "__main__" and not docker_mode:
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)