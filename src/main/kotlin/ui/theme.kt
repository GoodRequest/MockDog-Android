package ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

typealias M = Modifier
val C  @Composable get() = MaterialTheme.colors
val T  @Composable get() = MaterialTheme.typography
val SH @Composable get() = MaterialTheme.shapes

val PrimeBlack         = Color(0xFF05141F)
val PrimeBlackVariant  = Color(0xFF37434C)
val GreyLight          = Color(0xFFF0F1F2)
val GreyDark           = Color(0xFF121212)
val Red                = Color(0xFFEA0029)
val Black              = Color(0xFF000000)
val White              = Color(0xFFFFFFFF)
val BlueLight          = Color(0xFFE0F2FE)
val Blue               = Color(0xFF3B82F6)
val Green              = Color(0xFF66BB6A)
val Orange             = Color(0xFFFFC107)

val Palette = lightColors(
  primary          = PrimeBlack,
  primaryVariant   = PrimeBlackVariant,
  secondary        = PrimeBlack,
  secondaryVariant = PrimeBlackVariant,
  background       = GreyLight,
  surface          = White,
  error            = Red,
  onPrimary        = White,
  onSecondary      = PrimeBlack,
  onBackground     = PrimeBlack,
  onSurface        = PrimeBlack,
  onError          = White
)

val Typography = Typography(
  h1 = TextStyle(
    fontWeight    = FontWeight.Bold,
    fontSize      = 96.sp,
    letterSpacing = (-1.5).sp,
    lineHeight    = 116.sp
  ),
  h2 = TextStyle(
    fontWeight    = FontWeight.Bold,
    lineHeight    = 73.sp,
    fontSize      = 60.sp,
    letterSpacing = 0.sp
  ),
  h3 = TextStyle(
    fontWeight    = FontWeight.Bold,
    fontSize      = 48.sp,
    letterSpacing = 0.sp,
    lineHeight    = 58.sp
  ),
  h4 = TextStyle(
    fontWeight    = FontWeight.Bold,
    fontSize      = 34.sp,
    letterSpacing = 0.25.sp,
    lineHeight    = 41.sp
  ),
  h5 = TextStyle(
    fontWeight    = FontWeight.Bold,
    fontSize      = 24.sp,
    letterSpacing = 0.sp,
    lineHeight    = 29.sp
  ),
  h6 = TextStyle(
    fontWeight    = FontWeight.SemiBold,
    fontSize      = 20.sp,
    letterSpacing = 0.15.sp,
    lineHeight    = 28.sp
  ),
  subtitle1 = TextStyle(
    fontWeight    = FontWeight.Bold,
    fontSize      = 16.sp,
    letterSpacing = 0.15.sp,
    lineHeight    = 22.sp
  ),
  subtitle2 = TextStyle(
    fontWeight    = FontWeight.SemiBold,
    fontSize      = 14.sp,
    letterSpacing = 0.1.sp,
    lineHeight    = 20.sp
  ),
  body1 = TextStyle(
    fontWeight    = FontWeight.Normal,
    fontSize      = 16.sp,
    letterSpacing = 0.5.sp,
    lineHeight    = 24.sp
  ),
  body2 = TextStyle(
    fontWeight    = FontWeight.Normal,
    lineHeight    = 20.sp,
    fontSize      = 14.sp,
    letterSpacing = 0.5.sp
  ),
  button = TextStyle(
    fontWeight    = FontWeight.SemiBold,
    lineHeight    = 18.sp,
    fontSize      = 14.sp,
    letterSpacing = 0.2.sp
  ),
  caption = TextStyle(
    fontWeight    = FontWeight.Normal,
    fontSize      = 12.sp,
    letterSpacing = 0.4.sp,
    lineHeight    = 18.sp
  ),
  overline = TextStyle(
    fontWeight    = FontWeight.Medium,
    fontSize      = 10.sp,
    letterSpacing = 1.5.sp,
    lineHeight    = 16.sp
  )
)