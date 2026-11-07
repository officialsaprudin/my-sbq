package com.tpqsbq.mysbq.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpqsbq.mysbq.model.PembayaranItem
import com.tpqsbq.mysbq.util.AppUtils

val IndigoPrimary = Color(0xFF4338CA) // Indigo-700
val IndigoDark = Color(0xFF3730A3)    // Indigo-800
val IndigoLight = Color(0xFFEEF2FF)   // Indigo-50
val GreenSuccess = Color(0xFF16A34A)
val YellowWarn = Color(0xFFCA8A04)
val AmberPrimary = Color(0xFFD97706)  // Amber-600
val AmberLight = Color(0xFFFFFBEB)    // Amber-50
val OrangeSick = Color(0xFFEA580C)
val RedDanger = Color(0xFFDC2626)

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "Hadir" -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        "Izin" -> Color(0xFFFEF9C3) to Color(0xFFA16207)
        "Sakit" -> Color(0xFFFFEDD5) to Color(0xFFC2410C)
        "Alpa" -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        else -> Color(0xFFF3F4F6) to Color(0xFF374151)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status.ifBlank { "-" },
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SppStatusBadge(
    pembayaranList: List<PembayaranItem>,
    onClick: (() -> Unit)? = null
) {
    val bulanIni = remember { AppUtils.getBulanIniNama() }
    val sudahBayar = remember(pembayaranList, bulanIni) {
        pembayaranList.any { it.jenis == "SPP" && it.bulan == bulanIni }
    }

    val bgColor = if (sudahBayar) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
    val textColor = if (sudahBayar) Color(0xFF15803D) else Color(0xFFB45309)
    val icon = if (sudahBayar) Icons.Default.CheckCircle else Icons.Default.Schedule
    val text = if (sudahBayar) "Lunas ($bulanIni)" else "Belum Bayar"

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NilaiBadge(ket: String) {
    val clean = ket.ifBlank { "-" }
    val (bgColor, textColor, label) = when (clean) {
        "Lancar", "Lancar / Lanjut" -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Lancar")
        "Mengulang" -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "Ulang")
        "Kurang Lancar" -> Triple(Color(0xFFFEF9C3), Color(0xFFA16207), "Kurang")
        else -> Triple(Color(0xFFF3F4F6), Color(0xFF4B5563), clean)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (label.isNotEmpty()) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = selectedOption.ifBlank { "-" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal,
                            color = if (option == selectedOption) IndigoPrimary else Color.Black
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
