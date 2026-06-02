from fastapi import APIRouter
from ml_service import predict_engagement
from pydantic import BaseModel

router = APIRouter()

class PredictionRequest(BaseModel):
    messageTemplate: str
    startDate: str | None = None

@router.post("/predict-engagement")
async def predict(data: PredictionRequest):
    result = predict_engagement(data.messageTemplate, data.startDate)
    return result