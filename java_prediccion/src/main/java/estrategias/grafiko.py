import pandas as pd
import matplotlib.pyplot as plt

df = pd.read_csv("resultados_paralelizacion.csv")

plt.figure(figsize=(10, 6))
for estrategia in df["estrategia"].unique():
    datos = df[df["estrategia"] == estrategia]
    plt.plot(datos["num_parcelas"], datos["tiempo_ms"], marker='o', label=estrategia)

plt.title("Comparación de estrategias de paralelización")
plt.xlabel("Número de parcelas")
plt.ylabel("Tiempo de ejecución (ms)")
plt.legend()
plt.grid(True)
plt.tight_layout()
plt.show()