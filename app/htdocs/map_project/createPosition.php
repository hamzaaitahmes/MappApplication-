<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

// Configuration de la base de données
$host = "localhost";
$db_name = "map_project";
$username = "root";
$password = ""; // Laissez vide pour XAMPP, "root" pour MAMP

try {
    $conn = new PDO("mysql:host=$host;dbname=$db_name;charset=utf8mb4", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch(PDOException $e) {
    echo json_encode(["success" => false, "message" => "Erreur de connexion: " . $e->getMessage()]);
    exit;
}

// Récupération des données POST
$latitude = isset($_POST['latitude']) ? filter_var($_POST['latitude'], FILTER_VALIDATE_FLOAT) : null;
$longitude = isset($_POST['longitude']) ? filter_var($_POST['longitude'], FILTER_VALIDATE_FLOAT) : null;
$date = isset($_POST['date']) ? $_POST['date'] : null;
$imei = isset($_POST['imei']) ? $_POST['imei'] : null;

// Validation des données
if ($latitude === null || $longitude === null || $date === null) {
    echo json_encode(["success" => false, "message" => "Données manquantes"]);
    exit;
}

// Validation des coordonnées
if ($latitude < -90 || $latitude > 90 || $longitude < -180 || $longitude > 180) {
    echo json_encode(["success" => false, "message" => "Coordonnées invalides"]);
    exit;
}

try {
    $stmt = $conn->prepare(
        "INSERT INTO positions (latitude, longitude, date, imei)
         VALUES (:latitude, :longitude, :date, :imei)"
    );

    $stmt->bindParam(':latitude', $latitude);
    $stmt->bindParam(':longitude', $longitude);
    $stmt->bindParam(':date', $date);
    $stmt->bindParam(':imei', $imei);

    if ($stmt->execute()) {
        echo json_encode(["success" => true, "message" => "Position enregistrée avec succès"]);
    } else {
        echo json_encode(["success" => false, "message" => "Erreur lors de l'insertion"]);
    }
} catch(PDOException $e) {
    echo json_encode(["success" => false, "message" => "Erreur: " . $e->getMessage()]);
}
?>