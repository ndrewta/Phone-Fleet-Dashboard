from contextlib import asynccontextmanager
from datetime import datetime
from dotenv import load_dotenv
import os
from pydantic import BaseModel
from tapo import ApiClient
from fastapi import FastAPI
import json


load_dotenv()
CONFIG_PATH = os.getenv("DEVICE_CONFIG_FILE", "config/devices.json")


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