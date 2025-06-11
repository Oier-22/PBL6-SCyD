# PBL6-SCyD

Simulazio honetan bi karpeta daude, bat egin diren paralelizazio proba denak dauden karpeta eta bestea, behin paralelizazio teknika bat aukeratuta eta gero rabbitMQ integratua dagoen simulazioa. Simulazio arazorik gabe exekutatzeko "PBL6-SCYD" karpetatik exekutatu behar da, ez da "rabbitMQ" karpeta barrutik, bestela ez ditu behar dituen fitxategiak aurkituko eta.

Bestalde, "datu basea" deitzen karpeta barruan datuak sortzeko bi sql daude, bat datuen taulentzako da eta bestea datuak ausaz sortzeko (hemen egokitu zenbat datu sortu nahi duzuen datu kantitate, while baten bidez sortzen dira). Datu basearen izena aldatzen baduzue config.txt fitxategiak aldatu behar duzue izena datuak modu egokian jeisteko.

Horrez gain, erabiltzaile bat sortu beher izan zen rabbitMQ-rako bere pasahitzarekin, nire kasuan "testuser", kodea ondo exekutatu ahal izateko erabiltzaile berdina sortu beharko zen pasahitz berdinarekin edo beste erabiltzaile bat erabili eta config.txt fitxategian erabiltzailea eta bere pasahitza aldatu (berdina datu baseko erabiltzailearekin).
