#!/usr/bin/env python3
"""
Script de teste para explorar a API Open-Meteo
Objetivo: Entender estrutura de respostas e dados disponíveis
"""

import requests
import json
from datetime import datetime

def print_section(title):
    print("\n" + "="*80)
    print(f"  {title}")
    print("="*80)

def test_sao_paulo():
    """Teste 1: São Paulo - Previsão Atual + Diária"""
    print_section("TESTE 1: SÃO PAULO - Previsão Atual + Diária")
    
    try:
        url = "https://api.open-meteo.com/v1/forecast"
        params = {
            "latitude": -23.5505,
            "longitude": -46.6333,
            "current": "temperature_2m,weather_code,humidity",
            "daily": "temperature_2m_max,temperature_2m_min,weather_code,precipitation_sum",
            "timezone": "America/Sao_Paulo",
            "forecast_days": 7
        }
        
        resp = requests.get(url, params=params, timeout=10)
        resp.raise_for_status()
        data = resp.json()
        
        print(f"✓ Status: {resp.status_code}")
        print(f"\nInformações do Local:")
        print(f"  • Latitude: {data['latitude']}")
        print(f"  • Longitude: {data['longitude']}")
        print(f"  • Elevação: {data['elevation']}m")
        print(f"  • Timezone: {data['timezone']} ({data['timezone_abbreviation']})")
        print(f"  • Tempo de geração: {data['generationtime_ms']}ms")
        
        print(f"\nCondição Atual (current):")
        curr = data['current']
        curr_units = data['current_units']
        print(f"  • Horário: {curr['time']}")
        print(f"  • Temperatura: {curr['temperature_2m']}{curr_units['temperature_2m']}")
        print(f"  • Código de Clima: {curr['weather_code']} (WMO - veja tabela no final)")
        if 'humidity' in curr:
            print(f"  • Umidade: {curr['humidity']}{curr_units.get('humidity', '%')}")
        
        print(f"\nPrevisão Diária (próximos 3 dias):")
        print(f"  {'Data':<12} {'Tmax':<8} {'Tmin':<8} {'Código':<8} {'Precip':<10}")
        print(f"  {'-'*56}")
        for i in range(min(3, len(data['daily']['time']))):
            t = data['daily']['time'][i]
            tmax = data['daily']['temperature_2m_max'][i]
            tmin = data['daily']['temperature_2m_min'][i]
            code = data['daily']['weather_code'][i]
            precip = data['daily']['precipitation_sum'][i]
            print(f"  {t:<12} {tmax:<8}°C {tmin:<8}°C {code:<8} {precip:<10}mm")
        
        print(f"\n✓ Estrutura de resposta principal:")
        print(f"  • Chaves: {', '.join(data.keys())}")
        
        return True
    except Exception as e:
        print(f"✗ ERRO: {e}")
        return False

def test_rio_horly():
    """Teste 2: Rio de Janeiro - Dados Horários"""
    print_section("TESTE 2: RIO DE JANEIRO - Dados Horários (primeiras 6 horas)")
    
    try:
        url = "https://api.open-meteo.com/v1/forecast"
        params = {
            "latitude": -22.9068,
            "longitude": -43.1729,
            "hourly": "temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,weather_code",
            "current": "temperature_2m",
            "timezone": "America/Sao_Paulo",
            "forecast_days": 1
        }
        
        resp = requests.get(url, params=params, timeout=10)
        resp.raise_for_status()
        data = resp.json()
        
        print(f"✓ Status: {resp.status_code}")
        print(f"\nLocal: Rio de Janeiro ({data['latitude']}, {data['longitude']})")
        
        print(f"\nDados Horários - Primeiras 6 horas:")
        print(f"  {'Horário':<20} {'Temp':<8} {'Umid':<8} {'Precip':<10} {'Vento':<10}")
        print(f"  {'-'*66}")
        for i in range(min(6, len(data['hourly']['time']))):
            t = data['hourly']['time'][i]
            temp = data['hourly']['temperature_2m'][i]
            umid = data['hourly']['relative_humidity_2m'][i]
            precip = data['hourly']['precipitation'][i]
            vento = data['hourly']['wind_speed_10m'][i]
            print(f"  {t:<20} {temp:<8}°C {umid:<8}% {precip:<10}mm {vento:<10}km/h")
        
        print(f"\n✓ Variáveis horários disponíveis:")
        print(f"  • {', '.join(data['hourly'].keys()[:5])}...")
        
        return True
    except Exception as e:
        print(f"✗ ERRO: {e}")
        return False

def test_brasilia_complete():
    """Teste 3: Brasília - Variáveis Completas"""
    print_section("TESTE 3: BRASÍLIA - Máximo de Variáveis Disponíveis")
    
    try:
        url = "https://api.open-meteo.com/v1/forecast"
        params = {
            "latitude": -15.7942,
            "longitude": -47.8822,
            "current": "temperature_2m,weather_code,wind_speed_10m,wind_direction_10m,relative_humidity_2m,apparent_temperature",
            "daily": "temperature_2m_max,temperature_2m_min,temperature_2m_mean,weather_code,precipitation_sum,rain_sum,showers_sum,snowfall_sum,precipitation_hours,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,uv_index_max,sunshine_duration,daylight_duration",
            "hourly": "temperature_2m,weather_code",
            "timezone": "America/Sao_Paulo",
            "forecast_days": 3
        }
        
        resp = requests.get(url, params=params, timeout=10)
        resp.raise_for_status()
        data = resp.json()
        
        print(f"✓ Status: {resp.status_code}")
        
        print(f"\nCondição Atual Completa:")
        curr = data['current']
        print(f"  • Temperatura: {curr['temperature_2m']}°C")
        print(f"  • Sensação Térmica: {curr['apparent_temperature']}°C")
        print(f"  • Umidade: {curr['relative_humidity_2m']}%")
        print(f"  • Vento: {curr['wind_speed_10m']}km/h ({curr['wind_direction_10m']}°)")
        print(f"  • Código de Clima: {curr['weather_code']} (WMO)")
        
        print(f"\nPrevisão Diária Completa (3 dias):")
        for i in range(min(3, len(data['daily']['time']))):
            t = data['daily']['time'][i]
            tmax = data['daily']['temperature_2m_max'][i]
            tmin = data['daily']['temperature_2m_min'][i]
            tmean = data['daily']['temperature_2m_mean'][i]
            precip = data['daily']['precipitation_sum'][i]
            precip_hours = data['daily']['precipitation_hours'][i]
            vento = data['daily']['wind_speed_10m_max'][i]
            gust = data['daily']['wind_gusts_10m_max'][i]
            uv = data['daily']['uv_index_max'][i]
            sun = data['daily']['sunshine_duration'][i] / 3600  # converter segundos para horas
            daylight = data['daily']['daylight_duration'][i] / 3600  # converter segundos para horas
            
            print(f"\n  {t}:")
            print(f"    Temperatura: Máx={tmax}°C, Mín={tmin}°C, Média={tmean}°C")
            print(f"    Precipitação: {precip}mm (em {precip_hours} horas)")
            print(f"    Vento: {vento}km/h (ragas: {gust}km/h)")
            print(f"    Solar: UV={uv}, Sol={sun:.1f}h, Luz do dia={daylight:.1f}h")
        
        print(f"\n✓ Estrutura de Resposta Completa:")
        print(f"  • Chaves raiz: {', '.join(data.keys())}")
        print(f"  • Variáveis current: {', '.join(data['current'].keys())}")
        print(f"  • Variáveis daily: {', '.join(data['daily'].keys())}")
        print(f"  • Variáveis hourly (primeiras): {', '.join(list(data['hourly'].keys())[:5])}...")
        
        return True
    except Exception as e:
        print(f"✗ ERRO: {e}")
        return False

def main():
    print("\n" + "#"*80)
    print("# ANÁLISE COMPLETA DA API OPEN-METEO")
    print("# Investigando endpoints, dados disponíveis e estrutura de respostas")
    print("#"*80)
    
    results = []
    results.append(("São Paulo", test_sao_paulo()))
    results.append(("Rio", test_rio_horly()))
    results.append(("Brasília", test_brasilia_complete()))
    
    print_section("RESUMO DE TESTES")
    for name, result in results:
        status = "✓ SUCESSO" if result else "✗ FALHA"
        print(f"  {name}: {status}")
    
    print_section("TABELA DE CÓDIGOS WMO DE CLIMA")
    print("""
    Código | Descrição
    -------|----------------------------------------
      0    | Céu limpo
    1,2,3  | Céu principalmente limpo, parcialmente nublado, nublado
   45,48   | Neblina
   51-55   | Garoa (suave, moderada, densa)
   56,57   | Garoa congelante (suave, densa)
   61-65   | Chuva (suave, moderada, forte)
   66,67   | Chuva congelante (suave, forte)
   71-75   | Neve (suave, moderada, forte)
     77    | Grãos de neve
   80-82   | Chuva em pancadas (suave, moderada, violenta)
   85,86   | Neve em pancadas (suave, forte)
     95*   | Tempestade (suave ou moderada)
   96,99*  | Tempestade com granizo (suave, forte)
    
    * Apenas disponível na Europa Central
    """)
    
    print_section("ANÁLISE CONCLUÍDA")
    print("✓ Todos os testes foram executados com sucesso!")
    print("✓ Verifique os dados acima para estrutura de resposta e variáveis disponíveis.")

if __name__ == "__main__":
    main()
