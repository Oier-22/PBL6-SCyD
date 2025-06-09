import sys
import json
import joblib
import numpy as np
import os

# Cargar el modelo (ruta relativa segura)
ruta_modelo = os.path.join(os.path.dirname(__file__), "modelo_consumo.pkl")
modelo = joblib.load(ruta_modelo)

# Leer el JSON desde archivo temporal
with open(sys.argv[1], "r") as f:
    entrada = json.load(f)

# Crear array de entrada con los 10 parámetros esperados por el modelo
X = np.array([[
    entrada["temp"],
    entrada["humedad"],
    entrada["viento"],
    entrada["radiacion"],
    entrada["precipitacion"],
    entrada["tipo_planta"],
    entrada["etapa_crecimiento"],
    entrada["tipo_suelo"],
    entrada["humedad_suelo"],
    entrada["dia_del_ano"]
]])

# Realizar la predicción
pred = modelo.predict(X)[0]

# Imprimir resultado con dos decimales
print(f"{pred:.2f}")