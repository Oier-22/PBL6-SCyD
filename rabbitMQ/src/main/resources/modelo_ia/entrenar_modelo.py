from sklearn.ensemble import RandomForestRegressor
import joblib
import numpy as np

# Datos simulados: [temp, humedad_relativa, viento, radiacion, precipitacion, tipo_planta, etapa_crecimiento, tipo_suelo, humedad_suelo, dia_del_ano]
X = np.array([
    [30, 60, 2, 500, 0, 1, 2, 1, 30, 150],
    [25, 70, 1, 400, 5, 2, 3, 2, 45, 160],
    [35, 50, 3, 600, 0, 1, 1, 1, 25, 170],
    [20, 80, 1, 300, 10, 3, 2, 3, 50, 180],
    [32, 55, 2, 550, 0, 2, 3, 2, 35, 190]
])
y = np.array([25, 22, 30, 18, 26])  # Consumo de agua en L/m²

modelo = RandomForestRegressor(n_estimators=100, random_state=42)
modelo.fit(X, y)

joblib.dump(modelo, "modelo_consumo.pkl")
print("Modelo guardado.")