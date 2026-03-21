import asyncio
from contextlib import asynccontextmanager
from dotenv import load_dotenv
import os
from pydantic import BaseModel
from tapo import ApiClient
from fastapi import FastAPI
import uvicorn


load_dotenv()
device_list= {
}

class DeviceReport(BaseModel):
    device_id: str
    battery_level: int
    is_charging: bool
    plug_slot: int


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
    
    if device.device_id not in device_list:
        device_list[device.device_id] = {
            "battery_level": device.battery_level,
            "is_charging": device.is_charging,
            "plug_slot": device.plug_slot
        }

    device_list[device.device_id] = device.model_dump()
    
    if device.battery_level <= 20 and not device.is_charging:
        plug = await app.state.power_board.plug(position=device.plug_slot)
        await plug.on()
        print(f"Device {device.device_id} is low on battery. Plug {device.plug_slot} turned ON.")
    elif device.battery_level >= 80 and device.is_charging:
        plug = await app.state.power_board.plug(position=device.plug_slot)
        await plug.off()
        print(f"Device {device.device_id} is fully charged. Plug {device.plug_slot} turned OFF.")


if __name__ == "__main__":
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)